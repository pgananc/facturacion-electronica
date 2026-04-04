import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import {
  Company,
  Establishment,
} from '../../../_model/customer/company-establishment';
import { MatTableDataSource } from '@angular/material/table';
import { LoggerService } from '../../../_service/logger/logger.service';

@Component({
  selector: 'app-company-dialog',
  templateUrl: './company-dialog.component.html',
  styleUrls: ['./company-dialog.component.css'],
})
export class CompanyDialogComponent implements OnInit {
  companyDataSource: MatTableDataSource<Company>;
  selectedEstablishmentIds: { [idCompany: number]: number } = {};
  warningMessage: string | undefined;

  constructor(
    private readonly dialogRef: MatDialogRef<CompanyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) private readonly data: Company[] | { companies: Company[]; warningMessage?: string },
    private readonly logger: LoggerService
  ) {}

  ngOnInit(): void {
    // Manejar tanto el formato antiguo (array) como el nuevo (objeto con companies y warningMessage)
    let companies: Company[];
    if (Array.isArray(this.data)) {
      companies = this.data;
      this.warningMessage = undefined;
    } else {
      companies = this.data.companies;
      this.warningMessage = this.data.warningMessage;
    }

    this.companyDataSource = new MatTableDataSource(companies);

    // Log para diagnosticar el problema
    this.logger.debug('[CompanyDialog] Inicializando diálogo:', {
      companiesCount: companies.length,
      hasWarningMessage: !!this.warningMessage,
      warningMessage: this.warningMessage,
      companies: companies.map((c) => ({
        id: c.idCompany,
        name: c.name,
        totalEstablishments: c.establishments?.length || 0,
        activeEstablishments: this.getActiveEstablishments(c.establishments)
          .length,
        establishments: c.establishments?.map((e) => ({
          id: e.idEstablishment,
          name: e.name,
          code: e.code,
          status: e.status,
        })),
      })),
    });

    // Si hay una sola compañía con una sola sucursal activa, seleccionarla automáticamente
    if (companies.length === 1) {
      const company = companies[0];
      const activeEstablishments = this.getActiveEstablishments(
        company.establishments
      );

      // Log específico para diagnosticar el problema de sucursales
      this.logger.debug(
        '[CompanyDialog] Verificando sucursales para auto-selección:',
        {
          companyId: company.idCompany,
          companyName: company.name,
          totalEstablishments: company.establishments?.length || 0,
          activeEstablishmentsCount: activeEstablishments.length,
          activeEstablishments: activeEstablishments.map((e) => ({
            id: e.idEstablishment,
            name: e.name,
            code: e.code,
          })),
          allEstablishments:
            company.establishments?.map((e) => ({
              id: e.idEstablishment,
              name: e.name,
              code: e.code,
              status: e.status,
            })) || [],
        }
      );

      if (activeEstablishments.length === 1) {
        // Auto-seleccionar la única sucursal activa
        this.selectedEstablishmentIds[company.idCompany] =
          activeEstablishments[0].idEstablishment;
        this.logger.debug(
          '[CompanyDialog] Auto-seleccionando única sucursal activa:',
          {
            companyId: company.idCompany,
            companyName: company.name,
            establishmentId: activeEstablishments[0].idEstablishment,
            establishmentName: activeEstablishments[0].name,
          }
        );
      } else if (activeEstablishments.length === 0) {
        this.logger.warn(
          '[CompanyDialog] La compañía no tiene sucursales activas:',
          {
            companyId: company.idCompany,
            companyName: company.name,
            totalEstablishments: company.establishments?.length || 0,
          }
        );
      }
    }
  }

  /**
   * Filtra y retorna solo las sucursales activas
   */
  getActiveEstablishments(
    establishments: Establishment[] | undefined
  ): Establishment[] {
    if (!establishments) {
      return [];
    }
    return establishments.filter((e) => e.status === true);
  }

  /**
   * Maneja el cambio de selección de sucursal
   */
  onEstablishmentChange(companyId: number, establishmentId: number): void {
    this.selectedEstablishmentIds[companyId] = establishmentId;
    this.logger.debug('[CompanyDialog] Sucursal seleccionada:', {
      companyId,
      establishmentId,
      selectedEstablishmentIds: this.selectedEstablishmentIds,
    });
  }

  selectCompany(company: Company) {
    this.logger.debug('[CompanyDialog] Intentando seleccionar compañía:', {
      companyId: company?.idCompany,
      companyName: company?.name,
      selectedEstablishmentId: this.selectedEstablishmentIds[company.idCompany],
      hasSelectedEstablishment:
        !!this.selectedEstablishmentIds[company.idCompany],
    });

    if (company && company.idCompany != null && company.idCompany > 0) {
      const idEstablishment = this.selectedEstablishmentIds[company.idCompany];
      if (idEstablishment) {
        this.logger.debug('[CompanyDialog] Cerrando diálogo con selección:', {
          companyId: company.idCompany,
          companyName: company.name,
          idEstablishment,
        });
        this.dialogRef.close({
          company,
          idEstablishment,
        });
      } else {
        this.logger.warn(
          '[CompanyDialog] No se puede seleccionar compañía sin sucursal:',
          {
            companyId: company.idCompany,
            companyName: company.name,
            activeEstablishments: this.getActiveEstablishments(
              company.establishments
            ).length,
          }
        );
      }
    }
  }
}
