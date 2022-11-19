import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { User } from '../../../_model/auth/user';
import { UserService } from '../../../_service/user/user.service';
import { ActivatedRoute, Route, Router, Params } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RoleService } from '../../../_service/role/role.service';
import { Role } from '../../../_model/auth/role';
import { MatButtonModule } from '@angular/material/button';
import { switchMap } from 'rxjs';
import { UPDATE, SUCCESS } from '../../../_constants/constants';
import {
  EXIST_DATA,
  HEADER_MESSAGE,
  DURATION_TIME_MESSAGE,
} from '../../../_constants/constants';

@Component({
  selector: 'app-update-user',
  templateUrl: './update-user.component.html',
  styleUrls: ['./update-user.component.css'],
})
export class UpdateUserComponent implements OnInit {
  idUser: number = 0;
  user: User = new User();
  roles: Role[] = [];
  edition: boolean = false;
  idRole: Number = 0;
  userNameLast: String = '';
  form: FormGroup;

  constructor(
    private userService: UserService,
    private roleService: RoleService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.user = new User();
    this.form = new FormGroup({
      id: new FormControl(0),
      name: new FormControl(''),
      userName: new FormControl(''),
      password: new FormControl(''),
      passwordRepeat: new FormControl(''),
      email: new FormControl('', [Validators.email]),
      status: new FormControl(true),
    });

    this.findRoles();
    this.route.params.subscribe((params: Params) => {
      this.idUser = params['idUser'];
      this.edition = params['idUser'] != null;
      this.initForm();
    });
  }

  initForm() {
    if (this.edition) {
      this.userService.findById(this.idUser).subscribe((data) => {
        if (data.code == 0 && data.userDtos.length > 0) {
          let user = data.userDtos[0];
          this.idRole = user.roleDtos[0]?.idRole;
          this.userNameLast = user.userName;
          this.form = new FormGroup({
            id: new FormControl(user.idUser),
            name: new FormControl(user.name),
            userName: new FormControl(user.userName),
            password: new FormControl(user.password),
            passwordRepeat: new FormControl(user.password),
            email: new FormControl(user.mail),
            status: new FormControl(user.status),
          });
        }
      });
    }
  }

  findRoles() {
    this.roleService.findAll().subscribe((data) => {
      if (data.code == 0 && data.roleDtos.length > 0) {
        this.roles = data.roleDtos;
      }
    });
  }
  validateSave() {
    if (this.edition && this.userNameLast === this.form.value['userName']) {
      this.save();
    } else {
      this.validateUserName();
    }
  }

  validateUserName() {
    this.userService
      .existsByUserName(this.form.value['userName'])
      .subscribe((data) => {
        if (data) {
          this.snackBar.open(
            EXIST_DATA.MESSAGE_EXISTS_USER.message,
            HEADER_MESSAGE.MESSAGE_HEADER_INFO.message,
            {
              duration: DURATION_TIME_MESSAGE.value,
            }
          );
        } else {
          this.save();
        }
      });
  }

  save() {
    this.user.idUser = this.idUser;
    this.user.name = this.form.value['name'];
    this.user.userName = this.form.value['userName'];
    this.user.password = this.form.value['password'];
    this.user.status = this.form.value['status'];
    this.user.mail = this.form.value['email'];
    this.user.roleDtos = this.addRoles();
    if (this.user != null && this.user.idUser > 0) {
      this.userService
        .update(this.user, this.user.idUser)
        .pipe(
          switchMap(() => {
            return this.userService.findAll();
          })
        )
        .subscribe((data) => {
          this.userService.userChange.next(data.userDtos);
          this.userService.messangeChange.next(
            UPDATE.MESSAGE_UPDATE_USER.message
          );
        });
    } else {
      this.userService
        .save(this.user)
        .pipe(
          switchMap(() => {
            return this.userService.findAll();
          })
        )
        .subscribe((data) => {
          this.userService.userChange.next(data.userDtos);
          this.userService.messangeChange.next(
            SUCCESS.MESSAGE_REGISTER_CLIENT.message
          );
        });
    }

    this.router.navigate(['user']);
  }

  addRoles(): Role[] {
    let role = new Role();
    role.idRole = this.idRole;
    let roles = [];
    roles.push(role);
    return roles;
  }

  get f() {
    return this.form.controls;
  }
}
