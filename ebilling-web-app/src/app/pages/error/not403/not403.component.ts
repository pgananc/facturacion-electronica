import { Component, OnInit } from '@angular/core';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-not403',
  templateUrl: './not403.component.html',
  styleUrls: ['./not403.component.css'],
})
export class Not403Component implements OnInit {
  userName: string;
  constructor() {}

  ngOnInit(): void {
    this.userName = sessionStorage.getItem(environment.USER)!;
  }
}
