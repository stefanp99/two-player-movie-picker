import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { environment } from '../../environments/environment';
import { Movie } from '../models/movie.model';
import { HttpClient } from '@angular/common/http';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-main-page',
  imports: [
    CommonModule,

    FormsModule,
    ReactiveFormsModule,

    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatButtonModule,
    MatExpansionModule,
    MatDividerModule,

    MovieCardComponent
  ],
  templateUrl: './main-page.component.html',
  styleUrl: './main-page.component.css'
})
export class MainPageComponent {
  seed: string = '';

  initialMovies: Movie[] = [];

  seedForm: FormGroup;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.seedForm = this.fb.group({
      seed: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{4}$/)]]
    });
  }

  generateSeed() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < 4; i++) {
      const randomIndex = Math.floor(Math.random() * chars.length);
      result += chars[randomIndex];
    }
    this.seed = result;

    this.getRandomMovies();
  }

  inputSeed() {
    if (this.seedForm.valid) {
      this.seed = this.seedForm.value.seed;

      this.getRandomMovies();
    }
  }

  getRandomMovies() {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/fetch?language=en-US&limit=10&seed=${this.seed}`;//TODO: make lang and limit configurable

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

  formatInput(): void {
    const control = this.seedForm.get('seed');
    if (control) {
      const formatted = (control.value || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
      control.setValue(formatted, { emitEvent: false });
    }
  }
}
