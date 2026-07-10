import { Component } from '@angular/core';
import { FEATURED_EVENTS } from '../../../shared/data/home-content';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-event-section',
  imports: [TranslatePipe],
  templateUrl: './event-section.html',
  styleUrl: './event-section.scss',
})
export class EventSection {
  protected readonly events = FEATURED_EVENTS;
}
