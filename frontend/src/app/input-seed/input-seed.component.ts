import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { environment } from '../../environments/environment';
import { Movie } from '../models/movie.model';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-input-seed',
  imports: [
    CommonModule,

    FormsModule,
    ReactiveFormsModule,

    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatButtonModule,

    MovieCardComponent
  ],
  templateUrl: './input-seed.component.html',
  styleUrl: './input-seed.component.css'
})
export class InputSeedComponent {
  initialMovies: Movie[] = [];

  seedForm: FormGroup;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.seedForm = this.fb.group({
      seed: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{4}$/)]]
    });
  }

  formatInput(): void {
    const control = this.seedForm.get('seed');
    if (control) {
      const formatted = (control.value || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
      control.setValue(formatted, { emitEvent: false });
    }
  }

  getRandomMovies() {
    if (this.seedForm.valid) {
      const seed = this.seedForm.value.seed;
      const url = `${environment.apiBaseUrl}/api/v1/tmdb/fetch?language=en-US&limit=10&seed=${seed}`;//TODO: make lang and limit configurable

      this.http.get<Movie[]>(url).subscribe({
        next: response => {
          this.initialMovies = response;
          console.log('API Response:', this.initialMovies);
        },
        error: error => {
          console.error('API Error:', error);
        }
      });
    }
  }
}
