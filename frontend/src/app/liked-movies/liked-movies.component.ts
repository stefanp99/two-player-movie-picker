import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { LocalStorageService } from '../local-storage.service';

@Component({
  selector: 'app-liked-movies',
  imports: [
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './liked-movies.component.html',
  styleUrl: './liked-movies.component.css'
})
export class LikedMoviesComponent implements OnInit {
  @Output() return = new EventEmitter<void>();

  seed: String = '';

  constructor(private localStorageService: LocalStorageService) {
  }

  ngOnInit(): void {
    this.seed = this.localStorageService.getSeed();
  }

  back() {
    this.return.emit();
  }
}
