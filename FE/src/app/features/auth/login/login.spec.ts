import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, it, expect } from 'vitest';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginComponent } from './login';
import { AuthService } from '../auth.service';

describe('LoginComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AuthService,
      ],
    }).compileComponents();
  });

  it('should create the login component', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should initialize with an invalid form', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    expect(component.loginForm.valid).toBeFalsy();
  });

  it('should validate email format correctly', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const emailControl = component.loginForm.controls.email;

    emailControl.setValue('invalid-email');
    expect(emailControl.valid).toBeFalsy();
    expect(emailControl.errors?.['email']).toBeTruthy();

    emailControl.setValue('name@company.com');
    expect(emailControl.valid).toBeTruthy();
  });

  it('should toggle password visibility signal', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;

    expect(component.showPassword()).toBeFalsy();
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBeTruthy();
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBeFalsy();
  });
});
