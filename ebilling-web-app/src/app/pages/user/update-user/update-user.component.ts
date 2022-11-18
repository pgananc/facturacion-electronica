import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { User } from '../../../_model/auth/user';
import { UserService } from '../../../_service/user/user.service';
import { ActivatedRoute, Route, Router, Params } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  MatTreeFlatDataSource,
  MatTreeFlattener,
} from '@angular/material/tree';
import { TodoItemFlatNode } from '../../../_model/todoItemFlatNode';
import { FlatTreeControl } from '@angular/cdk/tree';
import { TodoItemNode } from '../../../_model/todoItemNode';
import { SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject } from 'rxjs';
import { IDENTIFICATION_TYPE } from '../../../_constants/constants';
import { RoleResponse } from 'src/app/_model/auth/roleResponse';
import { RoleService } from '../../../_service/role/role.service';
import { Role } from '../../../_model/auth/role';
import { MatButtonModule } from '@angular/material/button';

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
          console.log(user);
          this.idRole = user.roleDtos[0].idRole;

          this.userNameLast = user.userName;
          this.form = new FormGroup({
            id: new FormControl(user.idUser),
            name: new FormControl(user.name),
            userName: new FormControl(user.userName),
            password: new FormControl(user.userName),
            passwordRepeat: new FormControl(user.userName),
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
  validateSave() {}

  get f() {
    return this.form.controls;
  }
}
