import { Component } from '@angular/core';
import { PROMO_DEALS } from '../../../shared/data/home-content';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-deal-band',
  imports: [TranslatePipe],
  templateUrl: './deal-band.html',
  styleUrl: './deal-band.scss',
})
export class DealBand {
  protected readonly deals = PROMO_DEALS;
}

