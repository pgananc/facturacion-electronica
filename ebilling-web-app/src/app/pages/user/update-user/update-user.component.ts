import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {User} from '../../../_model/auth/user';
import {UserService} from '../../../_service/user/user.service';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';

import {RoleService} from '../../../_service/role/role.service';
import {Role} from '../../../_model/auth/role';
import {switchMap} from 'rxjs';
import {
  DURATION_TIME_MESSAGE,
  EXIST_DATA,
  HEADER_MESSAGE,
  SUCCESS,
  SUPER_ADMIN_ROLE,
  UPDATE
} from '../../../_constants/constants';
import {environment} from 'src/environments/environment';
import {decodeToken} from "../../../_functions/functions";

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
  ) {
  }

  ngOnInit(): void {
    this.user = new User();
    this.form = new FormGroup({
      id: new FormControl(0),
      name: new FormControl(''),
      identification: new FormControl(''),
      password: new FormControl(''),
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
            identification: new FormControl(user.userName),
            password: new FormControl(user.password),
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
        this.roles=this.roles.filter(role=>role.name!==SUPER_ADMIN_ROLE.name);
      }
    });
  }

  validateSave() {
    if (this.edition && this.userNameLast === this.form.value['email']) {
      this.save();
    } else {
      this.validateUserName();
    }
  }

  validateUserName() {
    const idCompany = Number(sessionStorage.getItem(environment.ID_COMPANY)!);
    this.userService
      .existsByIdCompanyAndUserName(idCompany, this.form.value['email'])
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
    const roles: [] = decodeToken().roles;
    let idCompany = 0;
    if (roles.find(role => role !== SUPER_ADMIN_ROLE)) {
      idCompany = parseInt(sessionStorage.getItem(environment.ID_COMPANY)!);
    } else {
      idCompany = parseInt(sessionStorage.getItem('companyIdCompanyUser')!);
    }
    console.log(`id company a guardar: ` + idCompany);
    this.user.idUser = this.idUser;
    this.user.name = this.form.value['name'];
    this.user.userName = this.form.value['identification'];
    this.user.password = this.form.value['password'];
    this.user.status = this.form.value['status'];
    this.user.mail = this.form.value['email'];
    this.user.idCompany = idCompany;
    this.user.roleDtos = this.addRoles();
    if (this.user != null && this.user.idUser > 0) {
      this.userService
        .update(this.user, this.user.idUser)
        .pipe(
          switchMap(() => {
            return this.userService.findByIdCompany(idCompany);
          })
        )
        .subscribe((data) => {
          this.userService.userChange.next(data.userDtos);
          this.userService.messageChange.next(
            UPDATE.MESSAGE_UPDATE_USER.message
          );
        });
    } else {
      this.userService
        .save(this.user)
        .pipe(
          switchMap(() => {
            return this.userService.findByIdCompany(idCompany);
          })
        )
        .subscribe((data) => {
          this.userService.userChange.next(data.userDtos);
          this.userService.messageChange.next(
            SUCCESS.MESSAGE_REGISTER_USER.message
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
