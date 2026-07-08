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

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'my-account', component: MyAccountComponent },
  { path: 'organization-dashboard', component: OrganizationDashboardComponent },
  { path: 'organizer/dashboard', component: OrganizerPortalComponent },
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
