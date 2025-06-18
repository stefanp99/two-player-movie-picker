import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InputSeedComponent } from './input-seed.component';

describe('InputSeedComponent', () => {
  let component: InputSeedComponent;
  let fixture: ComponentFixture<InputSeedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InputSeedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InputSeedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
