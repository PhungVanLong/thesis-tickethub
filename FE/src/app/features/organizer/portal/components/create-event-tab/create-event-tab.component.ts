import { Component, inject, OnInit, signal, computed, Output, EventEmitter } from '@angular/core';
import { Router } from '@angular/router';
import {
  FormBuilder, FormGroup, FormArray,
  ReactiveFormsModule, Validators, AbstractControl
} from '@angular/forms';
import { EventApiService } from '../../../../../core/services/event.service';
import { AuthService } from '../../../../auth/auth.service';
import { NgClass, CurrencyPipe, DecimalPipe, DatePipe, NgTemplateOutlet } from '@angular/common';
import { CdkDrag } from '@angular/cdk/drag-drop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-create-event-tab',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, CdkDrag, DatePipe, NgTemplateOutlet],
  templateUrl: './create-event-tab.component.html',
  styleUrl: './create-event-tab.component.scss',
})
export class CreateEventTabComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly eventApi = inject(EventApiService);
  private readonly sanitizer = inject(DomSanitizer);

  @Output() navigateTo = new EventEmitter<string>();

  readonly userProfile = this.authService.currentUserProfile;

  readonly currentStep = signal(1);
  readonly totalSteps = 4;
  readonly isSubmitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly submitSuccess = signal(false);

  readonly steps = [
    { label: 'Event Info' },
    { label: 'Ticket Tiers' },
    { label: 'Seat Map' },
    { label: 'Review & Submit' },
  ];

  readonly tierTypes = ['STANDING', 'SEATED'];

  // Step 1: Event Info (Merged with Venue)
  readonly step1Form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    category: ['', Validators.required],
    description: [''],
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
    venue: ['', Validators.required],
    city: ['', Validators.required],
    locationCoords: [''],
    bannerUrl: [''],
  });

  // Step 2: Ticket Tiers (FormArray)
  readonly step2Form: FormGroup = this.fb.group({
    tiers: this.fb.array([this.createTierGroup()]),
  });

  // Step 3: Seat Map Configuration
  readonly step3Form: FormGroup = this.fb.group({
    name: ['Main Layout', Validators.required],
  });

  // Free-Form Workspace State
  draggableItems = signal<{
    id: string;
    type: 'block' | 'stage';
    x: number;
    y: number;
    rows?: number;
    cols?: number;
    tierName?: string;
    labelPrefix?: string;
    rotation?: number;
    startCol?: number;
  }[]>([]);

  selectedItemId = signal<string | null>(null);

  get tiersArray(): FormArray {
    return this.step2Form.get('tiers') as FormArray;
  }

  createTierGroup(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      tierType: ['STANDING', Validators.required],
      price: [0, [Validators.required, Validators.min(0)]],
      quantityTotal: [100, [Validators.required, Validators.min(1)]],
      colorCode: ['#2563EB'],
      saleStartDate: [''],
      saleEndDate: ['']
    });
  }

  addTier(): void {
    this.tiersArray.push(this.createTierGroup());
  }

  removeTier(index: number): void {
    if (this.tiersArray.length > 1) {
      this.tiersArray.removeAt(index);
    }
  }

  getTierControl(index: number, field: string): AbstractControl {
    return this.tiersArray.at(index).get(field)!;
  }

  // --- Seat Map Logic ---

  getSeatedTiers(): any[] {
    return this.tiersArray.value.filter((t: any) => t.tierType === 'SEATED');
  }

  addStage(): void {
    const items = this.draggableItems();
    this.draggableItems.set([...items, {
      id: `stage-${Date.now()}`,
      type: 'stage',
      x: 350,
      y: 50
    }]);
  }

  addSeatBlock(): void {
    const items = this.draggableItems();
    const newItems = [...items, {
      id: `block-${Date.now()}`,
      type: 'block',
      x: 350,
      y: 200,
      rows: 5,
      cols: 10,
      labelPrefix: 'A',
      startCol: 1,
      tierName: this.getSeatedTiers()[0]?.name || '',
      rotation: 0
    }];
    this.draggableItems.set(this.recalculateSeatLayout(newItems));
  }

  removeDraggableItem(id: string): void {
    const newItems = this.draggableItems().filter(i => i.id !== id);
    this.draggableItems.set(this.recalculateSeatLayout(newItems));
    if (this.selectedItemId() === id) {
      this.selectedItemId.set(null);
    }
  }

  onDragEnded(event: any, item: any): void {
    const transform = event.source.getFreeDragPosition();
    const items = this.draggableItems();
    let newItems = items.map(i => i.id === item.id ? { ...i, x: transform.x, y: transform.y } : i);
    newItems = this.recalculateSeatLayout(newItems);
    this.draggableItems.set(newItems);
  }

  selectItem(id: string): void {
    this.selectedItemId.set(id);
  }

  updateSelectedItem(field: string, value: any): void {
    const id = this.selectedItemId();
    if (!id) return;
    
    let finalValue = value;
    if (field === 'rows' || field === 'cols' || field === 'rotation') {
      finalValue = Number(value) || 0;
    }
    if (field === 'labelPrefix') {
      finalValue = value.toUpperCase();
    }

    const items = this.draggableItems();
    let newItems = items.map(item => item.id === id ? { ...item, [field]: finalValue } : item);
    newItems = this.recalculateSeatLayout(newItems);

    this.draggableItems.set(newItems);
  }

  getSelectedItem(): any | null {
    return this.draggableItems().find(i => i.id === this.selectedItemId()) || null;
  }

  getArray(n: any): number[] {
    const size = Number(n) || 1;
    return Array(size).fill(0).map((_, i) => i);
  }

  getRowLabel(prefix: string | null | undefined, rowIndex: number): string {
    const p = prefix || 'A';
    if (p.length === 1) {
      const baseCode = p.charCodeAt(0);
      return String.fromCharCode(baseCode + rowIndex);
    }
    return `${p}${String.fromCharCode(65 + rowIndex)}`;
  }

  validateSeatLabels(blocks: any[], editingBlockId: string | null): { valid: boolean; duplicates: string[]; conflictingBlockIds: string[] } {
    const seatMap = new Map<string, string>(); 
    const duplicates = new Set<string>();
    const conflictingBlockIds = new Set<string>();

    for (const block of blocks) {
      if (block.type !== 'block') continue;
      
      for (let r = 0; r < (block.rows || 1); r++) {
        const rowLbl = this.getRowLabel(block.labelPrefix, r);
        for (let c = 0; c < (block.cols || 1); c++) {
          const seatNum = c + (block.startCol || 1);
          const seatCode = `${rowLbl}${seatNum}`;
          if (seatMap.has(seatCode)) {
            duplicates.add(seatCode);
            conflictingBlockIds.add(block.id);
            conflictingBlockIds.add(seatMap.get(seatCode)!);
          } else {
            seatMap.set(seatCode, block.id);
          }
        }
      }
    }

    return {
      valid: duplicates.size === 0,
      duplicates: Array.from(duplicates),
      conflictingBlockIds: Array.from(conflictingBlockIds)
    };
  }

  recalculateSeatLayout(items: any[]): any[] {
    const blocks = items.filter(i => i.type === 'block');
    if (blocks.length === 0) return items;

    // 1. Group by Y coordinate (Horizontal Bands)
    const Y_TOLERANCE = 40;
    const groups: { y: number; blocks: any[] }[] = [];

    for (const block of blocks) {
      let foundGroup = false;
      for (const group of groups) {
        if (Math.abs(block.y - group.y) <= Y_TOLERANCE) {
          group.blocks.push(block);
          group.y = group.blocks.reduce((sum, b) => sum + b.y, 0) / group.blocks.length;
          foundGroup = true;
          break;
        }
      }
      if (!foundGroup) {
        groups.push({ y: block.y, blocks: [block] });
      }
    }

    // Sort groups top to bottom
    groups.sort((a, b) => a.y - b.y);

    // 2. Intelligent Numbering
    const firstBlock = items.find(i => i.type === 'block');
    let nextPrefixCode = 65; // 'A'
    if (firstBlock && firstBlock.labelPrefix && firstBlock.labelPrefix.length === 1) {
       nextPrefixCode = firstBlock.labelPrefix.charCodeAt(0);
    }

    const updatedBlocks = new Map<string, any>();

    for (const group of groups) {
      // Sort blocks left to right
      group.blocks.sort((a, b) => a.x - b.x);

      let currentColOffset = 0;
      let maxRowsInGroup = 0;
      const groupPrefix = String.fromCharCode(nextPrefixCode);

      for (const block of group.blocks) {
        updatedBlocks.set(block.id, {
          ...block,
          labelPrefix: groupPrefix,
          startCol: currentColOffset + 1
        });
        currentColOffset += (block.cols || 1);
        if ((block.rows || 1) > maxRowsInGroup) {
          maxRowsInGroup = (block.rows || 1);
        }
      }

      nextPrefixCode += maxRowsInGroup;
    }

    return items.map(item => {
      if (item.type === 'block') {
        return updatedBlocks.get(item.id) || item;
      }
      return item;
    });
  }

  calculateAllocatedSeatsByTier(blocks: any[]): Record<string, number> {
    const allocated: Record<string, number> = {};
    for (const block of blocks) {
      if (block.type === 'block' && block.tierName) {
        allocated[block.tierName] = (allocated[block.tierName] || 0) + (block.rows || 1) * (block.cols || 1);
      }
    }
    return allocated;
  }

  validateTierCapacity(allocated: Record<string, number>, tiers: any[]): { valid: boolean; tierErrors: string[] } {
    const tierErrors: string[] = [];
    for (const tier of tiers) {
      if (tier.tierType === 'SEATED') {
        const alloc = allocated[tier.name] || 0;
        const capacity = Number(tier.quantityTotal) || 0;
        if (alloc > capacity) {
          tierErrors.push(`"${tier.name}" Tier Capacity Exceeded. Allocated: ${alloc} / ${capacity}. Exceeded by ${alloc - capacity} seats.`);
        }
      }
    }
    return { valid: tierErrors.length === 0, tierErrors };
  }

  readonly seatMapValidation = computed(() => {
    const items = this.draggableItems();
    const allocated = this.calculateAllocatedSeatsByTier(items);
    const capacityValidation = this.validateTierCapacity(allocated, this.tiersArray.value);
    const labelValidation = this.validateSeatLabels(items, this.selectedItemId());

    return {
      valid: labelValidation.valid && capacityValidation.valid,
      duplicates: labelValidation.duplicates,
      conflictingBlockIds: labelValidation.conflictingBlockIds,
      tierErrors: capacityValidation.tierErrors,
      allocatedByTier: allocated
    };
  });

  getBlockRowRange(item: any): string {
    if (item.type !== 'block' || !item.labelPrefix) return '';
    const start = this.getRowLabel(item.labelPrefix, 0);
    const end = this.getRowLabel(item.labelPrefix, (item.rows || 1) - 1);
    return start === end ? start : `${start}-${end}`;
  }

  getTierColor(tierName: string | null | undefined): string {
    if (!tierName) return 'transparent';
    const tier = this.tiersArray.value.find((t: any) => t.name === tierName);
    return tier?.colorCode || '#2563EB';
  }

  ngOnInit(): void {
    const profile = this.userProfile();
    if (profile) {
      const role = profile.role || '';
      if (!role.includes('ORGANIZER') && !role.includes('ADMIN')) {
        this.router.navigate(['/']);
      }
    }
  }

  goToStep(step: number): void {
    if (step >= 1 && step <= this.totalSteps) {
      this.currentStep.set(step);
    }
  }

  nextStep(): void {
    if (this.currentStep() < this.totalSteps) {
      this.currentStep.update(s => s + 1);
    }
  }

  prevStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.update(s => s - 1);
    }
  }

  buildPayload(): object {
    const s1 = this.step1Form.value;
    const tiers = this.tiersArray.value;
    const profile = this.userProfile();
    const organizationId = (profile as any)?.organizationId ?? 1;

    const allSeats: any[] = [];
    let seatMapConfig: any = { type: 'free-form', elements: this.draggableItems() };

    this.draggableItems().filter(i => i.type === 'block').forEach((block: any) => {
      for (let r = 0; r < (block.rows || 1); r++) {
        const rowLbl = this.getRowLabel(block.labelPrefix, r);
        for (let c = 0; c < (block.cols || 1); c++) {
          allSeats.push({
            seatCode: `${rowLbl}${c + (block.startCol || 1)}`,
            rowLabel: rowLbl,
            colNumber: c + (block.startCol || 1),
            ticketTierName: block.tierName
          });
        }
      }
    });

    const seatMaps = [];
    if (allSeats.length > 0) {
      seatMaps.push({
        name: this.step3Form.get('name')?.value || 'Main Layout',
        totalRows: 0,
        totalCols: 0,
        layoutJson: JSON.stringify(seatMapConfig),
        seats: allSeats
      });
    }

    return {
      organizationId,
      title: s1.title,
      description: s1.description || '',
      startTime: s1.startTime ? new Date(s1.startTime).toISOString() : null,
      endTime: s1.endTime ? new Date(s1.endTime).toISOString() : null,
      venue: s1.venue,
      city: s1.city,
      locationCoords: s1.locationCoords || null,
      bannerUrl: this.normalizeImageUrl(s1.bannerUrl) || null,
      ticketTiers: tiers.map((t: any) => ({
        name: t.name,
        tierType: t.tierType,
        price: t.price,
        quantityTotal: t.quantityTotal,
        colorCode: t.colorCode || null,
        saleStart: t.saleStartDate ? new Date(t.saleStartDate).toISOString() : null,
        saleEnd: t.saleEndDate ? new Date(t.saleEndDate).toISOString() : null
      })),
      seatMaps: seatMaps.length > 0 ? seatMaps : undefined
    };
  }

  onSubmitEvent(): void {
    if (this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.submitError.set(null);

    const payload = this.buildPayload();
    this.eventApi.createEvent(payload as any).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitSuccess.set(true);
      },
      error: (err: any) => {
        this.isSubmitting.set(false);
        this.submitError.set(err?.error?.message || 'Failed to create event. Please try again.');
      },
    });
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const gdriveRegex = /drive\.google\.com\/file\/d\/([^\/]+)/;
    const match = url.match(gdriveRegex);
    if (match && match[1]) {
      return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    }
    return url;
  }

  getNormalizedBannerUrl(): string {
    return this.normalizeImageUrl(this.step1Form.get('bannerUrl')?.value);
  }

  getTotalCapacity(): number {
    const tiers = this.tiersArray.value;
    return tiers.reduce((acc: number, t: any) => acc + (Number(t.quantityTotal) || 0), 0);
  }

  getSafeMapUrl(): SafeResourceUrl | null {
    const address = this.step1Form.get('city')?.value;
    if (!address) return null;
    
    const query = encodeURIComponent(address);
    const url = `https://maps.google.com/maps?q=${query}&t=&z=14&ie=UTF8&iwloc=&output=embed`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
