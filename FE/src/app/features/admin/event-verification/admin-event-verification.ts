import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { AdminService, EventResponse } from '../admin.service';

type EventDecision = 'APPROVED' | 'REJECTED';

interface ApproveModal {
  event: EventResponse;
  decision: EventDecision;
  reason: string;
}

@Component({
  selector: 'app-admin-event-verification',
  standalone: true,
  imports: [FormsModule, NgClass],
  templateUrl: './admin-event-verification.html',
  styleUrl: './admin-event-verification.scss',
})
export class AdminEventVerificationComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly events = signal<EventResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filterStatus = signal<string>('PENDING');
  readonly approveModal = signal<ApproveModal | null>(null);
  readonly submitting = signal(false);
  readonly submitSuccess = signal<string | null>(null);
  readonly detailEvent = signal<EventResponse | null>(null);

  readonly statusOptions = ['PENDING', 'APPROVED', 'PUBLISHED', 'CANCELLED'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminService.getEvents(this.filterStatus()).subscribe({
      next: (data) => {
        this.events.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load events');
        this.loading.set(false);
      }
    });
  }

  onFilterChange(status: string): void {
    this.filterStatus.set(status);
    this.load();
  }

  openApprove(event: EventResponse, decision: EventDecision): void {
    this.approveModal.set({ event, decision, reason: '' });
    this.submitSuccess.set(null);
  }

  closeApprove(): void {
    this.approveModal.set(null);
  }

  openDetail(event: EventResponse): void {
    this.detailEvent.set(event);
  }

  closeDetail(): void {
    this.detailEvent.set(null);
  }

  submitApproval(): void {
    const modal = this.approveModal();
    if (!modal) return;
    this.submitting.set(true);
    this.adminService.approveEvent(modal.event.id, {
      decision: modal.decision,
      reason: modal.reason || undefined
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.approveModal.set(null);
        this.submitSuccess.set(`Event "${modal.event.title}" has been ${modal.decision.toLowerCase()}.`);
        this.load();
        setTimeout(() => this.submitSuccess.set(null), 4000);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.error?.error || 'Approval failed. Please try again.');
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge-pending',
      APPROVED: 'badge-approved',
      PUBLISHED: 'badge-published',
      CANCELLED: 'badge-cancelled',
    };
    return map[status] || 'badge-pending';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }
}
