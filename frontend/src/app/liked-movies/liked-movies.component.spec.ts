import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LikedMoviesComponent } from './liked-movies.component';

describe('LikedMoviesComponent', () => {
  let component: LikedMoviesComponent;
  let fixture: ComponentFixture<LikedMoviesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LikedMoviesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LikedMoviesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
