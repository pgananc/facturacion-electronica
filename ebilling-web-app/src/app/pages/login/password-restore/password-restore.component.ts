import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../../_service/auth/login.service';
import { ActivatedRoute } from '@angular/router';
import { FormGroup, FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'app-password-restore',
  templateUrl: './password-restore.component.html',
  styleUrls: ['./password-restore.component.css'],
})
export class PasswordRestoreComponent implements OnInit {
  message: string;
  error: string;
  percentage: number = 0;
  form: FormGroup;

  constructor(
    private loginService: LoginService,
    public route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.form = new FormGroup({
      email: new FormControl('', [Validators.email]),
    });
  }

  sendMail() {
    this.percentage = 90;
    this.loginService.sendMail(this.form.value['email']).subscribe((data) => {
      if (data === 1) {
        this.message = 'Se enviaron las indicaciones al correo.';
        this.error = '';
        this.percentage = 100;
      } else {
        this.error = 'El correo ingresado no existe';
        this.percentage = 0;
      }
    });
  }

  get f() {
    return this.form.controls;
  }
}
