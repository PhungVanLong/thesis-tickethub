import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService, OrganizationResponse, EventResponse } from '../admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly pendingOrgs = signal<OrganizationResponse[]>([]);
  readonly pendingEvents = signal<EventResponse[]>([]);
  readonly loadingOrgs = signal(true);
  readonly loadingEvents = signal(true);
  readonly errorOrgs = signal<string | null>(null);
  readonly errorEvents = signal<string | null>(null);

  ngOnInit(): void {
    this.loadOrganizations();
    this.loadEvents();
  }

  private loadOrganizations(): void {
    this.adminService.getOrganizations('PENDING').subscribe({
      next: (data) => {
        this.pendingOrgs.set(data);
        this.loadingOrgs.set(false);
      },
      error: () => {
        this.errorOrgs.set('Failed to load organizations');
        this.loadingOrgs.set(false);
      }
    });
  }

  private loadEvents(): void {
    this.adminService.getEvents('PENDING').subscribe({
      next: (data) => {
        this.pendingEvents.set(data);
        this.loadingEvents.set(false);
      },
      error: () => {
        this.errorEvents.set('Failed to load events');
        this.loadingEvents.set(false);
      }
    });
  }

  get totalPending(): number {
    return this.pendingOrgs().length + this.pendingEvents().length;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const diff = Math.floor((Date.now() - d.getTime()) / 1000 / 60);
    if (diff < 60) return `${diff} minutes ago`;
    if (diff < 1440) return `${Math.floor(diff / 60)} hours ago`;
    return `${Math.floor(diff / 1440)} days ago`;
  }

  getInitial(name: string): string {
    return name ? name.charAt(0).toUpperCase() : '?';
  }
}
