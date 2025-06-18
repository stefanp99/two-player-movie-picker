import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { Movie } from '../models/movie.model';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-movie-card',
  imports: [
    CommonModule,

    MatCardModule,
    MatGridListModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './movie-card.component.html',
  styleUrl: './movie-card.component.css'
})
export class MovieCardComponent implements OnInit {
  @Input() movies: Movie[] = [];
  @Input() seed: string = '';
  index: number = 0;
  limit: number = 0;

  constructor(private http: HttpClient) {

  }

  ngOnInit(): void {
    this.limit = this.movies.length;
    console.log(this.limit)
  }

  skipNext() {
    this.index++;
    if (this.index % (this.limit / 2) == 0) {
      this.generateNewSeedAndLoadMoreMovies();
    }
  }

  favorite() {
    this.index++;
    if (this.index % (this.limit / 2) == 0) {
      this.generateNewSeedAndLoadMoreMovies();
    }
  }

  generateNewSeedAndLoadMoreMovies() {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/generate-seed/${this.seed}`;

    this.http.get(url, { responseType: 'text' }).subscribe({
      next: response => {
        this.seed = response;
        console.log('New Seed:', this.seed);
        this.loadMoreMovies(this.limit / 2)
      },
      error: error => {
        console.error('API Error:', error);
      }
    })

  }

  loadMoreMovies(limit: number) {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/fetch?language=en-US&limit=${limit}&seed=${this.seed}`;//TODO: make lang configurable

    this.http.get<Movie[]>(url).subscribe({
      next: response => {
        this.movies.push(...response);
        console.log('New List:', this.movies);
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

}
