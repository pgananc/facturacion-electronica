import { Component, OnInit } from '@angular/core';
import { environment } from 'src/environments/environment';
import { LoginService } from '../../../_service/auth/login.service';

@Component({
  selector: 'app-not404',
  templateUrl: './not404.component.html',
  styleUrls: ['./not404.component.css'],
})
export class Not404Component implements OnInit {
  userName: string;
  constructor(private loginService: LoginService) {}

  ngOnInit(): void {
    this.userName = sessionStorage.getItem(environment.USER)!;
  }
}
