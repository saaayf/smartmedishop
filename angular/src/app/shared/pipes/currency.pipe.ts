import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'currency'
})
export class CurrencyPipe implements PipeTransform {

  transform(value: number, currency: string = 'TND'): string {
    if (value == null) return '';
    
    return new Intl.NumberFormat('fr-TN', {
      style: 'currency',
      currency: currency
    }).format(value);
  }
}
