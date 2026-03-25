CREATE TABLE app_user (
                     id          BIGSERIAL PRIMARY KEY,
                     email       VARCHAR(255) NOT NULL UNIQUE,
                     password_hash VARCHAR(255) NOT NULL,
                     full_name   VARCHAR(255),
                     created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                     updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE desk (
                     id          BIGSERIAL PRIMARY KEY,
                     name        VARCHAR(255) NOT NULL,
                     location    VARCHAR(255)
);


CREATE TABLE booking (
                       id        BIGSERIAL PRIMARY KEY,
                       desk_id   BIGINT      NOT NULL,
                       user_id   BIGINT      NOT NULL,
                       start_at  TIMESTAMPTZ NOT NULL,
                       end_at    TIMESTAMPTZ NOT NULL,
                       CONSTRAINT fk_booking_desk
                         FOREIGN KEY (desk_id) REFERENCES desk (id)
                           ON DELETE CASCADE,
                       CONSTRAINT fk_booking_user
                         FOREIGN KEY (user_id) REFERENCES app_user (id)
                           ON DELETE CASCADE
);

CREATE TABLE recurring_booking_series (
  id BIGSERIAL PRIMARY KEY,
  desk_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  day_of_week SMALLINT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  occurrences INT NOT NULL,
  CONSTRAINT fk_series_desk FOREIGN KEY (desk_id) REFERENCES desk (id) ON DELETE CASCADE,
  CONSTRAINT fk_series_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

-- series_id is nullable so exisiting single bookings are unaffected 
ALTER TABLE booking
  ADD COLUMN series_id BIGINT,
  ADD CONSTRAINT fk_booking_series
    FOREIGN KEY (series_id) REFERENCES recurring_booking_series (id) ON DELETE CASCADE;