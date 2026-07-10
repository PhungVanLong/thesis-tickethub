import { Component } from '@angular/core';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-why-book',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './why-book.html',
  styleUrl: './why-book.scss',
})
export class WhyBook {}
