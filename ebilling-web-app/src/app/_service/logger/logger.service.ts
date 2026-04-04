import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  NONE = 4,
}

@Injectable({
  providedIn: 'root',
})
export class LoggerService {
  private currentLogLevel: LogLevel = environment.production
    ? LogLevel.ERROR
    : LogLevel.DEBUG;

  private readonly colorStyles = {
    debug: 'color: #7cb342; font-weight: bold',
    info: 'color: #29b6f6; font-weight: bold',
    warn: 'color: #ffa726; font-weight: bold',
    error: 'color: #ef5350; font-weight: bold',
  };

  /**
   * Log de debug - Solo visible en desarrollo
   * Equivalente a logger.debug() en Spring Boot
   */
  debug(message: string, ...optionalParams: any[]): void {
    this.log(LogLevel.DEBUG, message, optionalParams);
  }

  /**
   * Log de información general
   * Equivalente a logger.info() en Spring Boot
   */
  info(message: string, ...optionalParams: any[]): void {
    this.log(LogLevel.INFO, message, optionalParams);
  }

  /**
   * Log de advertencia
   * Equivalente a logger.warn() en Spring Boot
   */
  warn(message: string, ...optionalParams: any[]): void {
    this.log(LogLevel.WARN, message, optionalParams);
  }

  /**
   * Log de error
   * Equivalente a logger.error() en Spring Boot
   */
  error(message: string, error?: any, ...optionalParams: any[]): void {
    this.log(LogLevel.ERROR, message, [error, ...optionalParams]);
  }

  /**
   * Log de tabla - útil para mostrar arrays/objetos
   */
  table(data: any, message?: string): void {
    if (this.currentLogLevel <= LogLevel.DEBUG) {
      if (message) {
        console.log(
          `%c[TABLE] ${this.getTimestamp()} - ${message}`,
          this.colorStyles.debug
        );
      }
      console.table(data);
    }
  }

  /**
   * Agrupa logs relacionados
   */
  group(title: string): void {
    if (this.currentLogLevel <= LogLevel.DEBUG) {
      console.group(
        `%c[GROUP] ${this.getTimestamp()} - ${title}`,
        this.colorStyles.debug
      );
    }
  }

  /**
   * Cierra un grupo de logs
   */
  groupEnd(): void {
    if (this.currentLogLevel <= LogLevel.DEBUG) {
      console.groupEnd();
    }
  }

  /**
   * Método privado para hacer el log
   */
  private log(level: LogLevel, message: string, params: any[]): void {
    if (this.shouldLog(level)) {
      const timestamp = this.getTimestamp();
      const levelName = LogLevel[level];
      const style = this.getStyleForLevel(level);

      const logMessage = `%c[${levelName}] ${timestamp} - ${message}`;

      switch (level) {
        case LogLevel.DEBUG:
          console.log(logMessage, style, ...params);
          break;
        case LogLevel.INFO:
          console.info(logMessage, style, ...params);
          break;
        case LogLevel.WARN:
          console.warn(logMessage, style, ...params);
          break;
        case LogLevel.ERROR:
          console.error(logMessage, style, ...params);
          break;
      }
    }
  }

  private shouldLog(level: LogLevel): boolean {
    return level >= this.currentLogLevel;
  }

  private getTimestamp(): string {
    const now = new Date();
    return now.toISOString().split('T')[1].split('.')[0]; // HH:mm:ss
  }

  private getStyleForLevel(level: LogLevel): string {
    switch (level) {
      case LogLevel.DEBUG:
        return this.colorStyles.debug;
      case LogLevel.INFO:
        return this.colorStyles.info;
      case LogLevel.WARN:
        return this.colorStyles.warn;
      case LogLevel.ERROR:
        return this.colorStyles.error;
      default:
        return '';
    }
  }

  /**
   * Cambia el nivel de log en runtime (útil para debugging)
   */
  setLogLevel(level: LogLevel): void {
    this.currentLogLevel = level;
    this.info(`Log level changed to: ${LogLevel[level]}`);
  }
}
