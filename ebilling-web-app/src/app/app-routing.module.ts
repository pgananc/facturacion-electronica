import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { WelcomeComponent } from './pages/welcome/welcome.component';
import { ClientComponent } from './pages/customer/client/client.component';
import { UpdateClientComponent } from './pages/customer/client/update-client/update-client.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'welcome', component: WelcomeComponent },
  {
    path: 'client',
    component: ClientComponent,
    children: [
      { path: 'new', component: UpdateClientComponent },
      { path: 'edit/:idClient', component: UpdateClientComponent },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
