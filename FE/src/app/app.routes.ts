import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home';
import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password';
import { MyAccountComponent } from './features/auth/my-account/my-account';
import { OrganizationDashboardComponent } from './features/organizer/dashboard/dashboard';
import { OrganizerPortalComponent } from './features/organizer/portal/portal';
import { AdminShellComponent } from './features/admin/shell/admin-shell';
import { AdminDashboardComponent } from './features/admin/dashboard/admin-dashboard';
import { AdminOrganizationVerificationComponent } from './features/admin/organization-verification/admin-organization-verification';
import { AdminEventVerificationComponent } from './features/admin/event-verification/admin-event-verification';
import { EventDetailsComponent } from './features/home/event-details/event-details';
import { ExploreEventsComponent } from './features/home/explore-events/explore-events';
import { SeatSelectionComponent } from './features/home/seat-selection/seat-selection';
import { CheckoutComponent } from './features/booking/checkout/checkout';
import { TicketDetailComponent } from './features/booking/ticket-detail/ticket-detail';
import { StaffCheckInComponent } from './features/staff/check-in/staff-check-in';
import { canDeactivateGuard } from './core/guards/can-deactivate.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'explore', component: ExploreEventsComponent },
  { path: 'event/:id', component: EventDetailsComponent },
  { path: 'event/:id/booking', component: SeatSelectionComponent },
  { path: 'checkout/:orderId', component: CheckoutComponent, canDeactivate: [canDeactivateGuard] },
  { path: 'tickets/:ticketCode', component: TicketDetailComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'my-account', component: MyAccountComponent },
  { path: 'organization-dashboard', component: OrganizationDashboardComponent },
  { path: 'organizer/dashboard', component: OrganizerPortalComponent },
  { path: 'staff', redirectTo: 'staff/check-in', pathMatch: 'full' },
  { path: 'staff/check-in', component: StaffCheckInComponent },
  {
    path: 'admin',
    component: AdminShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'organizations', component: AdminOrganizationVerificationComponent },
      { path: 'events', component: AdminEventVerificationComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];
