What I Built

I added a weekly recurring desk booking. The approach was overall pretty simple as recurring bookings are just regular booking rows in the existing table, with a new table that ties them together. That way the booked slots just show up as booked. 

The main design decision I considered was what happens when you try to book a month, but week 3 has a conflict. I validated all occurrences first before saving anything. 


What I spent time on

This was my first time working with Micronaut and Kotlin. It was a fair amount of videos and diving into the documents to draft the backend fixes I wanted to implement, such as the TemporalAdjusters and the repository pattern.

The frontend came easier as typescript is in my comfort zone. I added a recurring toggle to the booking modal that reveals a day of week selector and a weeks input.

Known Issues

It was on reviewing the instructions where I saw the testing requirment. As this was a 2-3 hour scope(which I admittedly went over on), but I wanted to showcase my testing, I did add a frontend test. I would prefer to go back and do a backend test as well but with my time restriction I thought to skip it.

what I would do next

- Frontend test coverage beyond just the checkbox toggle
- implement Backend testing
- Add proper error handling instead of 500
- Dive deeper into Micronaut and Kotlin docs to iterate on my code as I'm sure there are better ways to implement