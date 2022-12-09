import { UserService } from './../../../../_service/user/user.service';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { LoginService } from '../../../../_service/auth/login.service';
import { ConfirmPasswordValidator } from './confirm-password.validator';
import { TokenService } from 'src/app/_service/auth/token.service';

@Component({
  selector: 'app-token',
  templateUrl: './token.component.html',
  styleUrls: ['./token.component.css'],
})
export class TokenComponent implements OnInit {
  form: FormGroup;
  token: string;
  message: string;
  error: string;
  response: number;
  validToken: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private userService: UserService,
    private tokenService: TokenService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group(
      {
        password: [''],
        confirmPassword: [''],
      },
      {
        validator: ConfirmPasswordValidator('password', 'confirmPassword'),
      }
    );

    this.route.params.subscribe(
      (params: Params) => {
        this.token = params['token'];
        this.tokenService.validateTokenReset(this.token).subscribe((data) => {
          if (data === true) {
            this.validToken = true;
          } else {
            this.validToken = false;
            setTimeout(() => {
              this.router.navigate(['login']);
            }, 2000);
          }
        });
      },
      (err) => {}
    );
  }

  onSubmit() {
    let clave: string = this.form.value.confirmPassword;
    this.userService.resetPassword(this.token, clave).subscribe((data) => {
      if (data === true) {
        this.message = 'Se cambio la contraseña exitosamente.';
        setTimeout(() => {
          this.router.navigate(['login']);
        }, 2000);
      }
    });
  }
}
