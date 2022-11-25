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
  mensaje: string;
  error: string;
  rpta: number;
  tokenValido: boolean;

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
        this.tokenValido = true;
        this.tokenService.validateTokenReset(this.token).subscribe((data) => {
          if (data === true) {
            this.tokenValido = true;
          } else {
            this.tokenValido = false;
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
    console.log(clave);
    this.userService.restore(this.token, clave).subscribe((data) => {
      if (data === true) {
        this.mensaje = 'Se cambio la contraseña';

        setTimeout(() => {
          this.router.navigate(['login']);
        }, 2000);
      }
    });
  }
}
