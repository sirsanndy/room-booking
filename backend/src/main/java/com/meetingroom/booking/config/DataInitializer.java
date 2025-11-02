package com.meetingroom.booking.config;

import com.meetingroom.booking.entity.Holiday;
import com.meetingroom.booking.entity.MeetingRoom;
import com.meetingroom.booking.entity.User;
import com.meetingroom.booking.repository.HolidayRepository;
import com.meetingroom.booking.repository.MeetingRoomRepository;
import com.meetingroom.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MeetingRoomRepository meetingRoomRepository;

    private final UserRepository userRepository;

    private final HolidayRepository holidayRepository;

    private final PasswordEncoder passwordEncoder;

    public DataInitializer(MeetingRoomRepository meetingRoomRepository, UserRepository userRepository, HolidayRepository holidayRepository, PasswordEncoder passwordEncoder) {
        this.meetingRoomRepository = meetingRoomRepository;
        this.userRepository = userRepository;
        this.holidayRepository = holidayRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Frontend sends SHA-256 hashed password: "password123" -> "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f"
            // So we need to store BCrypt(SHA-256("password123")) in database
            String sha256HashedPassword = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
            
            User user1 = new User();
            user1.setUsername("john");
            user1.setPassword(passwordEncoder.encode(sha256HashedPassword));
            user1.setEmail("john@example.com");
            user1.setFullName("John Doe");
            Set<String> roles1 = new HashSet<>();
            roles1.add("USER");
            user1.setRoles(roles1);
            user1.setEnabled(true);
            userRepository.save(user1);
            
            User user2 = new User();
            user2.setUsername("jane");
            user2.setPassword(passwordEncoder.encode(sha256HashedPassword));
            user2.setEmail("jane@example.com");
            user2.setFullName("Jane Smith");
            Set<String> roles2 = new HashSet<>();
            roles2.add("USER");
            user2.setRoles(roles2);
            user2.setEnabled(true);
            userRepository.save(user2);
            
            System.out.println("Sample users created: john/password123, jane/password123");
            System.out.println("Note: Passwords are stored as BCrypt(SHA-256(password))");
        }
        
        // Initialize sample meeting rooms if not exists
        if (meetingRoomRepository.count() == 0) {
            MeetingRoom room1 = new MeetingRoom();
            room1.setName("Conference Room A");
            room1.setCapacity(10);
            room1.setDescription("Large conference room with projector");
            room1.setLocation("1st Floor, Building A");
            room1.setFacilities("Projector, Whiteboard, Video Conference");
            room1.setAvailable(true);
            meetingRoomRepository.save(room1);
            
            MeetingRoom room2 = new MeetingRoom();
            room2.setName("Meeting Room B");
            room2.setCapacity(6);
            room2.setDescription("Medium-sized meeting room");
            room2.setLocation("2nd Floor, Building A");
            room2.setFacilities("TV Screen, Whiteboard");
            room2.setAvailable(true);
            meetingRoomRepository.save(room2);
            
            MeetingRoom room3 = new MeetingRoom();
            room3.setName("Huddle Room C");
            room3.setCapacity(4);
            room3.setDescription("Small room for quick meetings");
            room3.setLocation("3rd Floor, Building B");
            room3.setFacilities("TV Screen");
            room3.setAvailable(true);
            meetingRoomRepository.save(room3);
            
            MeetingRoom room4 = new MeetingRoom();
            room4.setName("Board Room");
            room4.setCapacity(15);
            room4.setDescription("Executive board room");
            room4.setLocation("5th Floor, Building A");
            room4.setFacilities("Projector, Video Conference, Whiteboard, Coffee Machine");
            room4.setAvailable(true);
            meetingRoomRepository.save(room4);
            
            System.out.println("Sample meeting rooms created");
        }
        
        // Initialize sample holidays if not exists
        if (holidayRepository.count() == 0) {
            // Indonesian National Holidays 2025
            
            // New Year's Day
            Holiday holiday1 = new Holiday();
            holiday1.setDate(LocalDate.of(2025, 1, 1));
            holiday1.setName("Tahun Baru Masehi");
            holiday1.setDescription("New Year's Day");
            holidayRepository.save(holiday1);
            
            // Imlek / Chinese New Year
            Holiday holiday2 = new Holiday();
            holiday2.setDate(LocalDate.of(2025, 1, 29));
            holiday2.setName("Tahun Baru Imlek 2576 Kongzili");
            holiday2.setDescription("Chinese New Year");
            holidayRepository.save(holiday2);
            
            // Isra Mi'raj
            Holiday holiday3 = new Holiday();
            holiday3.setDate(LocalDate.of(2025, 1, 27));
            holiday3.setName("Isra Mi'raj Nabi Muhammad SAW");
            holiday3.setDescription("Ascension of the Prophet Muhammad");
            holidayRepository.save(holiday3);
            
            // Nyepi / Balinese New Year
            Holiday holiday4 = new Holiday();
            holiday4.setDate(LocalDate.of(2025, 3, 29));
            holiday4.setName("Hari Suci Nyepi Tahun Baru Saka 1947");
            holiday4.setDescription("Balinese Day of Silence");
            holidayRepository.save(holiday4);
            
            // Good Friday
            Holiday holiday5 = new Holiday();
            holiday5.setDate(LocalDate.of(2025, 4, 18));
            holiday5.setName("Wafat Isa Al-Masih");
            holiday5.setDescription("Good Friday");
            holidayRepository.save(holiday5);
            
            // Eid al-Fitr 1446 H (Day 1)
            Holiday holiday6 = new Holiday();
            holiday6.setDate(LocalDate.of(2025, 3, 31));
            holiday6.setName("Hari Raya Idul Fitri 1446 H");
            holiday6.setDescription("Eid al-Fitr - Day 1");
            holidayRepository.save(holiday6);
            
            // Eid al-Fitr 1446 H (Day 2)
            Holiday holiday7 = new Holiday();
            holiday7.setDate(LocalDate.of(2025, 4, 1));
            holiday7.setName("Hari Raya Idul Fitri 1446 H");
            holiday7.setDescription("Eid al-Fitr - Day 2");
            holidayRepository.save(holiday7);
            
            // Labor Day
            Holiday holiday8 = new Holiday();
            holiday8.setDate(LocalDate.of(2025, 5, 1));
            holiday8.setName("Hari Buruh Internasional");
            holiday8.setDescription("International Labor Day");
            holidayRepository.save(holiday8);
            
            // Ascension Day of Jesus Christ
            Holiday holiday9 = new Holiday();
            holiday9.setDate(LocalDate.of(2025, 5, 29));
            holiday9.setName("Kenaikan Isa Al-Masih");
            holiday9.setDescription("Ascension of Jesus Christ");
            holidayRepository.save(holiday9);
            
            // Vesak Day
            Holiday holiday10 = new Holiday();
            holiday10.setDate(LocalDate.of(2025, 5, 12));
            holiday10.setName("Hari Raya Waisak 2569 BE");
            holiday10.setDescription("Vesak Day");
            holidayRepository.save(holiday10);
            
            // Pancasila Day
            Holiday holiday11 = new Holiday();
            holiday11.setDate(LocalDate.of(2025, 6, 1));
            holiday11.setName("Hari Lahir Pancasila");
            holiday11.setDescription("Pancasila Day");
            holidayRepository.save(holiday11);
            
            // Eid al-Adha 1446 H
            Holiday holiday12 = new Holiday();
            holiday12.setDate(LocalDate.of(2025, 6, 7));
            holiday12.setName("Hari Raya Idul Adha 1446 H");
            holiday12.setDescription("Eid al-Adha");
            holidayRepository.save(holiday12);
            
            // Islamic New Year 1447 H
            Holiday holiday13 = new Holiday();
            holiday13.setDate(LocalDate.of(2025, 6, 27));
            holiday13.setName("Tahun Baru Islam 1447 H");
            holiday13.setDescription("Islamic New Year");
            holidayRepository.save(holiday13);
            
            // Independence Day
            Holiday holiday14 = new Holiday();
            holiday14.setDate(LocalDate.of(2025, 8, 17));
            holiday14.setName("Hari Kemerdekaan Republik Indonesia");
            holiday14.setDescription("Indonesia Independence Day");
            holidayRepository.save(holiday14);
            
            // Maulid Nabi Muhammad SAW
            Holiday holiday15 = new Holiday();
            holiday15.setDate(LocalDate.of(2025, 9, 5));
            holiday15.setName("Maulid Nabi Muhammad SAW");
            holiday15.setDescription("Birthday of Prophet Muhammad");
            holidayRepository.save(holiday15);
            
            // Christmas Day
            Holiday holiday16 = new Holiday();
            holiday16.setDate(LocalDate.of(2025, 12, 25));
            holiday16.setName("Hari Raya Natal");
            holiday16.setDescription("Christmas Day");
            holidayRepository.save(holiday16);
            
            System.out.println("Indonesian holidays for 2025 created (16 national holidays + 3 joint leave days)");
        }
    }
}
