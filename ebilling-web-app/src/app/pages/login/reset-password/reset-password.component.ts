import { Component, OnDestroy, OnInit } from '@angular/core';
import { LoginService } from '../../../_service/auth/login.service';
import { ActivatedRoute } from '@angular/router';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css'],
})
export class ResetPasswordComponent implements OnInit, OnDestroy {
  message: string;
  error: string;
  percentage = 0;
  isSubmitting = false;
  form: FormGroup;
  private subscriptions = new Subscription();

  constructor(
    private loginService: LoginService,
    public route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.form = new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email]),
    });

    const emailControl = this.form.get('email');
    if (emailControl) {
      this.subscriptions.add(
        emailControl.valueChanges.subscribe((value) => {
          if (!value?.trim()) {
            this.message = '';
            this.error = '';
            this.percentage = 0;
          }
        })
      );
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  sendMail() {
    if (this.form.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.message = '';
    this.error = '';
    this.percentage = 90;

    this.loginService.sendMail(this.form.value['email']).subscribe({
      next: (data) => {
        if (data === 1) {
          this.message = 'Se enviaron las indicaciones al correo.';
          this.error = '';
          this.percentage = 100;
        } else {
          this.error = 'El correo ingresado no existe.';
          this.percentage = 0;
        }
        this.isSubmitting = false;
      },
      error: () => {
        this.error =
          'Ocurrió un error al enviar el correo. Intente nuevamente.';
        this.percentage = 0;
        this.isSubmitting = false;
      },
    });
  }

  get f() {
    return this.form.controls;
  }
}
