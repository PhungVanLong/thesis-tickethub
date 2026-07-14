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
  readonly safeMapUrl = signal<SafeResourceUrl | null>(null);

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
    customPrefix?: boolean;
    rotation?: number;
    startCol?: number;
  }[]>([]);

  selectedItemId = signal<string | null>(null);
  workspaceZoom = signal<number>(1);

  readonly reviewZoom = signal<number>(1);
  reviewIsDraggingMap = false;
  reviewDragStartX = 0;
  reviewDragStartY = 0;
  reviewDragScrollLeft = 0;
  reviewDragScrollTop = 0;

  readonly reviewMapStyle = computed(() => {
    const items = this.draggableItems();
    if (items.length === 0) {
      return { transform: 'scale(1)', left: '0px', top: '0px', position: 'absolute' };
    }

    let minX = Infinity, maxX = -Infinity;
    let minY = Infinity, maxY = -Infinity;

    items.forEach((item: any) => {
      const w = item.type === 'stage' ? 300 : (item.cols || 10) * 22 + 12;
      const h = item.type === 'stage' ? 60 : (item.rows || 5) * 22 + 12;
      if (item.x < minX) minX = item.x;
      if (item.x + w > maxX) maxX = item.x + w;
      if (item.y < minY) minY = item.y;
      if (item.y + h > maxY) maxY = item.y + h;
    });

    if (minX === Infinity) {
      return { transform: 'scale(1)', left: '0px', top: '0px', position: 'absolute' };
    }

    const mapW = maxX - minX;
    const mapH = maxY - minY;

    const viewportW = 900;
    const viewportH = 500;

    const scaleX = (viewportW - 60) / mapW;
    const scaleY = (viewportH - 60) / mapH;
    const baseScale = Math.min(scaleX, scaleY, 1.0);
    const scale = baseScale * this.reviewZoom();

    const offsetX = (viewportW - mapW * scale) / 2 - minX * scale;
    const offsetY = (viewportH - mapH * scale) / 2 - minY * scale;

    return {
      transform: `scale(${scale})`,
      transformOrigin: 'top left',
      left: `${offsetX}px`,
      top: `${offsetY}px`,
      position: 'absolute'
    };
  });

  reviewZoomIn(): void {
    this.reviewZoom.update(z => Math.min(z + 0.1, 3));
  }

  reviewZoomOut(): void {
    this.reviewZoom.update(z => Math.max(z - 0.1, 0.2));
  }

  reviewResetZoom(): void {
    this.reviewZoom.set(1);
  }

  reviewOnWheelZoom(event: WheelEvent): void {
    if (event.ctrlKey || event.metaKey) {
      event.preventDefault();
      if (event.deltaY < 0) {
        this.reviewZoomIn();
      } else {
        this.reviewZoomOut();
      }
    }
  }

  reviewOnMouseDown(e: MouseEvent): void {
    this.reviewIsDraggingMap = true;
    const currentTarget = e.currentTarget as HTMLElement;
    this.reviewDragStartX = e.pageX - currentTarget.offsetLeft;
    this.reviewDragStartY = e.pageY - currentTarget.offsetTop;
    this.reviewDragScrollLeft = currentTarget.scrollLeft;
    this.reviewDragScrollTop = currentTarget.scrollTop;
    currentTarget.style.cursor = 'grabbing';
  }

  reviewOnMouseLeave(e: MouseEvent): void {
    this.reviewIsDraggingMap = false;
    const currentTarget = e.currentTarget as HTMLElement;
    currentTarget.style.cursor = 'grab';
  }

  reviewOnMouseUp(e: MouseEvent): void {
    this.reviewIsDraggingMap = false;
    const currentTarget = e.currentTarget as HTMLElement;
    currentTarget.style.cursor = 'grab';
  }

  reviewOnMouseMove(e: MouseEvent): void {
    if (!this.reviewIsDraggingMap) return;
    e.preventDefault();
    const currentTarget = e.currentTarget as HTMLElement;
    const x = e.pageX - currentTarget.offsetLeft;
    const y = e.pageY - currentTarget.offsetTop;
    const walkX = (x - this.reviewDragStartX);
    const walkY = (y - this.reviewDragStartY);
    currentTarget.scrollLeft = this.reviewDragScrollLeft - walkX;
    currentTarget.scrollTop = this.reviewDragScrollTop - walkY;
  }

  zoomIn(): void {
    this.workspaceZoom.update(z => Math.min(z + 0.1, 2));
  }

  zoomOut(): void {
    this.workspaceZoom.update(z => Math.max(z - 0.1, 0.2));
  }

  resetZoom(): void {
    this.workspaceZoom.set(1);
  }

  onWheelZoom(event: WheelEvent): void {
    if (event.ctrlKey || event.metaKey) {
      event.preventDefault();
      if (event.deltaY < 0) {
        this.zoomIn();
      } else {
        this.zoomOut();
      }
    }
  }

  onMouseDown(e: MouseEvent): void {
    const target = e.target as HTMLElement;
    if (target.closest('.draggable-item')) return; // let cdkDrag handle item dragging
    
    this.isDraggingMap = true;
    const currentTarget = e.currentTarget as HTMLElement;
    this.dragStartX = e.pageX - currentTarget.offsetLeft;
    this.dragStartY = e.pageY - currentTarget.offsetTop;
    this.dragScrollLeft = currentTarget.scrollLeft;
    this.dragScrollTop = currentTarget.scrollTop;
    currentTarget.style.cursor = 'grabbing';
  }

  onMouseLeave(e: MouseEvent): void {
    this.isDraggingMap = false;
    const currentTarget = e.currentTarget as HTMLElement;
    currentTarget.style.cursor = 'default';
  }

  onMouseUp(e: MouseEvent): void {
    this.isDraggingMap = false;
    const currentTarget = e.currentTarget as HTMLElement;
    currentTarget.style.cursor = 'default';
  }

  onMouseMove(e: MouseEvent): void {
    if (!this.isDraggingMap) return;
    e.preventDefault();
    const currentTarget = e.currentTarget as HTMLElement;
    const x = e.pageX - currentTarget.offsetLeft;
    const y = e.pageY - currentTarget.offsetTop;
    const walkX = (x - this.dragStartX);
    const walkY = (y - this.dragStartY);
    currentTarget.scrollLeft = this.dragScrollLeft - walkX;
    currentTarget.scrollTop = this.dragScrollTop - walkY;
  }

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

  // Panning State
  isDraggingMap = false;
  dragStartX = 0;
  dragStartY = 0;
  dragScrollLeft = 0;
  dragScrollTop = 0;

  getCenterCoords(): { x: number, y: number } {
    const container = document.querySelector('.grid-canvas-container');
    if (container) {
      const rect = container.getBoundingClientRect();
      const zoom = this.workspaceZoom();
      const x = (container.scrollLeft + rect.width / 2) / zoom;
      const y = (container.scrollTop + rect.height / 2) / zoom;
      return { x: Math.max(0, x - 100), y: Math.max(0, y - 50) };
    }
    return { x: 1500, y: 1500 };
  }

  addStage(): void {
    const items = this.draggableItems();
    const pos = this.getCenterCoords();
    this.draggableItems.set([...items, {
      id: `stage-${Date.now()}`,
      type: 'stage',
      x: pos.x,
      y: pos.y
    }]);
  }

  addSeatBlock(): void {
    const items = this.draggableItems();
    const pos = this.getCenterCoords();
    const newItems = [...items, {
      id: `block-${Date.now()}`,
      type: 'block',
      x: pos.x,
      y: pos.y,
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
    let isCustomPrefix = false;

    if (field === 'rows' || field === 'cols' || field === 'rotation') {
      finalValue = Number(value) || 0;
    }
    if (field === 'labelPrefix') {
      finalValue = value.toUpperCase();
      isCustomPrefix = true;
    }

    const items = this.draggableItems();
    let newItems = items.map(item => item.id === id ? { 
      ...item, 
      [field]: finalValue,
      ...(isCustomPrefix ? { customPrefix: true } : {})
    } : item);
    
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
    let nextPrefixCode = 65; // 'A'
    const firstBlock = items.find(i => i.type === 'block');
    if (firstBlock && firstBlock.labelPrefix && firstBlock.labelPrefix.length === 1 && !firstBlock.customPrefix) {
       nextPrefixCode = firstBlock.labelPrefix.charCodeAt(0);
    }

    const updatedBlocks = new Map<string, any>();

    for (const group of groups) {
      // Sort blocks left to right
      group.blocks.sort((a, b) => a.x - b.x);

      let groupPrefix = String.fromCharCode(nextPrefixCode);
      
      const customBlock = group.blocks.find(b => b.customPrefix);
      if (customBlock && customBlock.labelPrefix) {
        groupPrefix = customBlock.labelPrefix;
        if (groupPrefix.length === 1) {
          nextPrefixCode = groupPrefix.charCodeAt(0);
        }
      }

      let currentColOffset = 0;
      let maxRowsInGroup = 0;

      for (const block of group.blocks) {
        const blockPrefix = block.customPrefix ? block.labelPrefix : groupPrefix;
        updatedBlocks.set(block.id, {
          ...block,
          labelPrefix: blockPrefix,
          startCol: currentColOffset + 1
        });
        currentColOffset += (block.cols || 1);
        if ((block.rows || 1) > maxRowsInGroup) {
          maxRowsInGroup = (block.rows || 1);
        }
      }

      if (groupPrefix.length === 1) {
         nextPrefixCode = groupPrefix.charCodeAt(0) + maxRowsInGroup;
      }
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

    this.step1Form.get('venue')?.valueChanges.subscribe(venue => {
      if (!venue) {
        this.safeMapUrl.set(null);
        return;
      }
      const query = encodeURIComponent(venue);
      const url = `https://maps.google.com/maps?q=${query}&t=&z=14&ie=UTF8&iwloc=&output=embed`;
      this.safeMapUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
    });
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
      category: s1.category,
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

}
