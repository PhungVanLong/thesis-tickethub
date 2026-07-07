import { Component } from '@angular/core';
import { FEATURED_EVENTS } from '../../../shared/data/home-content';

@Component({
  selector: 'app-event-section',
  templateUrl: './event-section.html',
  styleUrl: './event-section.scss',
})
export class EventSection {
  protected readonly events = FEATURED_EVENTS;
}
