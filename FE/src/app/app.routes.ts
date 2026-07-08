import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home';
import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password';
import { MyAccountComponent } from './features/auth/my-account/my-account';
import { OrganizationDashboardComponent } from './features/organizer/dashboard/dashboard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'my-account', component: MyAccountComponent },
  { path: 'organization-dashboard', component: OrganizationDashboardComponent },
];

