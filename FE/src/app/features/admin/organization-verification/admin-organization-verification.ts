import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { AdminService, OrganizationResponse } from '../admin.service';

type VerifyDecision = 'ACTIVE' | 'REJECTED' | 'BANNED';

interface VerifyModal {
  org: OrganizationResponse;
  decision: VerifyDecision;
  reason: string;
}

@Component({
  selector: 'app-admin-organization-verification',
  standalone: true,
  imports: [FormsModule, NgClass],
  templateUrl: './admin-organization-verification.html',
  styleUrl: './admin-organization-verification.scss',
})
export class AdminOrganizationVerificationComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly organizations = signal<OrganizationResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filterStatus = signal<string>('PENDING');
  readonly verifyModal = signal<VerifyModal | null>(null);
  readonly submitting = signal(false);
  readonly submitSuccess = signal<string | null>(null);
  readonly detailOrg = signal<OrganizationResponse | null>(null);

  readonly statusOptions = ['PENDING', 'ACTIVE', 'REJECTED', 'BANNED'];

  get listTitle(): string {
    const status = this.filterStatus();
    const statusName = status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
    return `${statusName} Verification`;
  }

  get listBadgeText(): string {
    const status = this.filterStatus();
    const count = this.organizations().length;
    if (status === 'PENDING') {
      return `${count} Requests`;
    }
    const statusName = status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
    return `${count} ${statusName}`;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminService.getOrganizations(this.filterStatus()).subscribe({
      next: (data) => {
        this.organizations.set(data);
        this.loading.set(false);
        if (data.length > 0) {
          // Auto select first organization
          this.detailOrg.set(data[0]);
        } else {
          this.detailOrg.set(null);
        }
      },
      error: () => {
        this.error.set('Failed to load organizations');
        this.loading.set(false);
      }
    });
  }

  onFilterChange(status: string): void {
    this.filterStatus.set(status);
    this.load();
  }

  selectOrg(org: OrganizationResponse): void {
    this.detailOrg.set(org);
  }

  openVerify(org: OrganizationResponse, decision: VerifyDecision): void {
    this.verifyModal.set({ org, decision, reason: '' });
    this.submitSuccess.set(null);
  }

  closeVerify(): void {
    this.verifyModal.set(null);
  }

  submitVerification(): void {
    const modal = this.verifyModal();
    if (!modal) return;
    this.submitting.set(true);
    this.adminService.verifyOrganization(modal.org.id, {
      decision: modal.decision,
      reason: modal.reason || undefined
    }).subscribe({
      next: () => {
        this.verifyModal.set(null);
        this.submitSuccess.set(`Organization "${modal.org.name}" has been ${modal.decision.toLowerCase()}.`);
        
        // Find index of verified organization
        const currentList = this.organizations();
        const index = currentList.findIndex(o => o.id === modal.org.id);
        
        // Re-load list
        this.adminService.getOrganizations(this.filterStatus()).subscribe({
          next: (data) => {
            this.organizations.set(data);
            this.loading.set(false);
            this.submitting.set(false);
            
            if (data.length > 0) {
              // Select next organization in list
              const nextIndex = index < data.length ? index : data.length - 1;
              this.detailOrg.set(data[nextIndex]);
            } else {
              this.detailOrg.set(null);
            }
          },
          error: () => {
            this.loading.set(false);
            this.submitting.set(false);
          }
        });
        
        setTimeout(() => this.submitSuccess.set(null), 4000);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.error?.error || 'Verification failed. Please try again.');
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge-pending',
      ACTIVE: 'badge-active',
      REJECTED: 'badge-rejected',
      BANNED: 'badge-banned',
    };
    return map[status] || 'badge-pending';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  }

  formatDateMonthDay(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric'
    });
  }

  getDisplayUrl(url: string | undefined): string {
    if (!url) return '';
    return url.replace(/https?:\/\/(www\.)?/, '');
  }

  getAvatarBgColor(name: string): string {
    const hash = this.hashCode(name);
    const colors = [
      '#1a1f2c', // Dark/Black like Live Nation
      '#0052cc', // Blue like Local Music
      '#d97706', // Orange
      '#4b5563', // Gray
      '#4f46e5', // Purple
      '#0891b2', // Cyan
    ];
    return colors[Math.abs(hash) % colors.length];
  }

  private hashCode(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    return hash;
  }

  getInitial(name: string): string {
    return name ? name.charAt(0).toUpperCase() : '?';
  }
}

