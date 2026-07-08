import { Component, inject, OnInit, signal, HostListener } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  FormBuilder, FormGroup, FormArray,
  ReactiveFormsModule, Validators, AbstractControl
} from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../auth/auth.service';
import { LanguageService } from '../../../core/services/language.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { NgClass, UpperCasePipe, CurrencyPipe, DecimalPipe } from '@angular/common';
import { CdkDrag } from '@angular/cdk/drag-drop';

@Component({
  selector: 'app-organizer-portal',
  standalone: true,
  imports: [ReactiveFormsModule, UpperCasePipe, DecimalPipe, CdkDrag],
  templateUrl: './portal.html',
  styleUrl: './portal.scss',
})
export class OrganizerPortalComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly langService = inject(LanguageService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  readonly userProfile = this.authService.currentUserProfile;
  readonly activeTab = signal('dashboard');
  readonly currentLang = this.langService.currentLang;
  readonly showLangDropdown = signal(false);

  readonly currentStep = signal(1);
  readonly totalSteps = 6;
  readonly isSubmitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly submitSuccess = signal(false);

  readonly steps = [
    { label: 'Event Info' },
    { label: 'Venue' },
    { label: 'Ticket Tiers' },
    { label: 'Seat Map' },
    { label: 'Review' },
    { label: 'Submit' },
  ];

  readonly tierTypes = ['STANDING', 'SEATED'];

  // Step 1: Event Info
  readonly step1Form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    category: ['', Validators.required],
    description: [''],
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
  });

  // Step 2: Venue
  readonly step2Form: FormGroup = this.fb.group({
    venue: ['', Validators.required],
    city: ['', Validators.required],
    locationCoords: [''],
    bannerUrl: [''],
  });

  // Step 3: Ticket Tiers (FormArray)
  readonly step3Form: FormGroup = this.fb.group({
    tiers: this.fb.array([this.createTierGroup()]),
  });

  // Step 4: Seat Map Configuration
  readonly step4Form: FormGroup = this.fb.group({
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
  }[]>([]);

  selectedItemId = signal<string | null>(null);

  get tiersArray(): FormArray {
    return this.step3Form.get('tiers') as FormArray;
  }

  createTierGroup(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      tierType: ['STANDING', Validators.required],
      price: [0, [Validators.required, Validators.min(0)]],
      quantityTotal: [100, [Validators.required, Validators.min(1)]],
      colorCode: ['#2563EB'],
      saleStart: [''],
      saleEnd: [''],
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
    this.draggableItems.set([...items, {
      id: `block-${Date.now()}`,
      type: 'block',
      x: 350,
      y: 200,
      rows: 5,
      cols: 10,
      labelPrefix: 'A',
      tierName: this.getSeatedTiers()[0]?.name || ''
    }]);
  }

  removeDraggableItem(id: string): void {
    this.draggableItems.set(this.draggableItems().filter(i => i.id !== id));
    if (this.selectedItemId() === id) {
      this.selectedItemId.set(null);
    }
  }

  onDragEnded(event: any, item: any): void {
    // CDK Drag Drop event contains distance moved, but we bind via [cdkDragFreeDragPosition]
    // So we don't strictly need to track unless we want to persist exact pixel visually.
    // However, to capture the final offset relative to boundary:
    const transform = event.source.getFreeDragPosition();
    const items = [...this.draggableItems()];
    const idx = items.findIndex(i => i.id === item.id);
    if (idx !== -1) {
      items[idx].x = transform.x;
      items[idx].y = transform.y;
      this.draggableItems.set(items);
    }
  }

  selectItem(id: string): void {
    this.selectedItemId.set(id);
  }

  updateSelectedItem(field: string, value: any): void {
    const id = this.selectedItemId();
    if (!id) return;
    const items = [...this.draggableItems()];
    const idx = items.findIndex(i => i.id === id);
    if (idx !== -1) {
      (items[idx] as any)[field] = value;
      this.draggableItems.set(items);
    }
  }

  getSelectedItem(): any {
    return this.draggableItems().find(i => i.id === this.selectedItemId()) || null;
  }

  getTierColor(tierName: string | null): string {
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

  openCreateEvent(): void {
    this.currentStep.set(1);
    this.step1Form.reset();
    this.step2Form.reset();
    this.step3Form.setControl('tiers', this.fb.array([this.createTierGroup()]));
    this.submitError.set(null);
    this.submitSuccess.set(false);
    this.activeTab.set('create-event');
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

  // Build final payload from all steps
  buildPayload(): object {
    const s1 = this.step1Form.value;
    const s2 = this.step2Form.value;
    const tiers = this.tiersArray.value;

    const profile = this.userProfile();
    // organizationId: lấy từ profile nếu có, tạm dùng 1 để test
    const organizationId = (profile as any)?.organizationId ?? 1;

    // Collect seats from drag & drop blocks
    const allSeats: any[] = [];
    let seatMapConfig: any = { type: 'free-form', elements: [] };

    this.draggableItems().forEach(item => {
      // save into layout config to redraw later
      seatMapConfig.elements.push(item);

      if (item.type === 'block') {
        const r = item.rows || 1;
        const c = item.cols || 1;
        const pref = item.labelPrefix || 'A';
        const tier = item.tierName;
        
        for (let rowIdx = 0; rowIdx < r; rowIdx++) {
          const rowLabel = String.fromCharCode(pref.charCodeAt(0) + rowIdx);
          for (let colIdx = 0; colIdx < c; colIdx++) {
            // Compute absolute approx coordinates (seat width 30px, spacing 5px)
            const absoluteX = item.x + (colIdx * 35);
            const absoluteY = item.y + (rowIdx * 35);

            allSeats.push({
              seatCode: `${rowLabel}${colIdx + 1}`,
              rowLabel: rowLabel,
              colNumber: colIdx + 1,
              ticketTierName: tier,
              coordinates: `${absoluteX},${absoluteY}`
            });
          }
        }
      }
    });

    const seatMaps = [];
    if (allSeats.length > 0) {
      seatMaps.push({
        name: this.step4Form.get('name')?.value || 'Main Layout',
        totalRows: 0, // Using free form
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
      venue: s2.venue,
      city: s2.city,
      locationCoords: s2.locationCoords || null,
      bannerUrl: s2.bannerUrl || null,
      ticketTiers: tiers.map((t: any) => ({
        name: t.name,
        tierType: t.tierType,
        price: t.price,
        quantityTotal: t.quantityTotal,
        colorCode: t.colorCode || null,
        saleStart: t.saleStart ? new Date(t.saleStart).toISOString() : null,
        saleEnd: t.saleEnd ? new Date(t.saleEnd).toISOString() : null,
      })),
      seatMaps: seatMaps.length > 0 ? seatMaps : undefined
    };
  }

  onSubmitEvent(): void {
    if (this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.submitError.set(null);

    const payload = this.buildPayload();
    this.http.post('http://localhost:8080/api/events/create', payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitSuccess.set(true);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.submitError.set(err?.error?.message || 'Failed to create event. Please try again.');
      },
    });
  }

  toggleLangDropdown(event: Event): void {
    event.stopPropagation();
    this.showLangDropdown.update(v => !v);
  }

  setLanguage(lang: 'Vie' | 'Eng'): void {
    this.langService.setLanguage(lang);
    this.showLangDropdown.set(false);
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showLangDropdown.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get userInitial(): string {
    const name = this.userProfile()?.fullName || this.userProfile()?.email || 'O';
    return name.charAt(0).toUpperCase();
  }

  // Keep old refs for dashboard tab compatibility
  readonly createEventForm: FormGroup = this.step1Form;
}
