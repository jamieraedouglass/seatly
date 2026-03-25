CREATE TABLE recurring_booking_series (
    id          BIGSERIAL PRIMARY KEY,
    desk_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_time  TIME     NOT NULL,
    end_time    TIME     NOT NULL,
    occurrences INT      NOT NULL,
    CONSTRAINT fk_series_desk FOREIGN KEY (desk_id) REFERENCES desk (id) ON DELETE CASCADE,
    CONSTRAINT fk_series_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

ALTER TABLE booking
    ADD COLUMN series_id BIGINT,
    ADD CONSTRAINT fk_booking_series
        FOREIGN KEY (series_id) REFERENCES recurring_booking_series (id) ON DELETE CASCADE;