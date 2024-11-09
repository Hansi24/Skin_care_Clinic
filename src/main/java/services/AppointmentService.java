package services;

import dao.AppointmentDAO;
import models.K2462921_Appointment;
import models.K2462921_Patient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class  AppointmentService {
    private final HashMap<String, ArrayList<K2462921_Appointment>> appointmentsByDate = new HashMap<>();

    private final HashMap<String, Double> treatmentPrices = new HashMap<>();
    private int appointmentCounter = 1;
    private static final double REGISTRATION_FEE = 500.00;
    private static final double TAX_RATE = 0.025;

    private AppointmentDAO appointmentDAO = new AppointmentDAO();


    public AppointmentService() {
        treatmentPrices.put("Acne Treatment", 2750.00);
        treatmentPrices.put("Skin Whitening", 7650.00);
        treatmentPrices.put("Mole Removal", 3850.00);
        treatmentPrices.put("Laser Treatment", 12500.00);
    }

    public void displayMenu() {
        Scanner scanner = new Scanner(System.in);
        int option;

        do {
            System.out.println("1. Make an Appointment");
            System.out.println("2. Search Appointments");
            System.out.println("3. Update Appointment");
            System.out.println("4. View Appointments by Date");
            System.out.println("5. Exit");
            System.out.print("Select an option: ");
            option = scanner.nextInt();

            switch (option) {
                case 1:
                    makeAppointment();
                    break;
                case 2:
                    searchAppointments();
                    break;
                case 3:
                    startUpdateProcess();
                    break;
                case 4:
                    viewAppointmentsByDate();
                    break;
                case 5:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        } while (option != 5);
    }


    public void makeAppointment() {
        Scanner scanner = new Scanner(System.in);
        AppointmentDAO appointmentDAO = new AppointmentDAO();

        Optional<Integer> lastId = appointmentDAO.getLastAppointmentId();
        int newId = lastId.map(id -> id + 1).orElse(1);
        String appointmentId = "" + newId;

        try {
            // Select a doctor
            System.out.println("\n--- Select Dermatologist ---");
            System.out.println("1. Dr. Ariyathunga");
            System.out.println("2. Dr. Jayaweera");
            System.out.print("Enter choice: ");
            String dermatologist = switch (scanner.nextInt()) {
                case 1 -> "Dr. Ariyathunga";
                case 2 -> "Dr. Jayaweera";
                default -> throw new IllegalArgumentException("Invalid choice for dermatologist.");
            };
            scanner.nextLine();

            // Select a Date
            System.out.print("\nEnter Date (yyyy-mm-dd): ");
            String date = scanner.nextLine();

            // Display available time slots based on the day
            String dayOfWeek = LocalDate.parse(date).getDayOfWeek().toString();
            List<LocalTime> availableSlots = getAvailableSlots(dayOfWeek);

            if (availableSlots.isEmpty()) {
                System.out.println("No available slots on " + dayOfWeek + ". Choose another day.");
                return;
            }

            // Display and Select Time Slot
            System.out.println("Available Time Slots:");
            for (int i = 0; i < availableSlots.size(); i++) {
                LocalTime start = availableSlots.get(i);
                LocalTime end = start.plusMinutes(15);
                System.out.printf("%d. %s - %s%n", (i + 1), start.format(DateTimeFormatter.ofPattern("hh:mm a")), end.format(DateTimeFormatter.ofPattern("hh:mm a")));
            }

            LocalTime selectedTime = null;
            while (true) {
                System.out.print("Select Time Slot (Enter number): ");
                int slotChoice = scanner.nextInt();
                if (slotChoice > 0 && slotChoice <= availableSlots.size()) {
                    selectedTime = availableSlots.get(slotChoice - 1);
                    String timeSlot = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"));

                    // Validate if the selected time slot is available for the chosen date
                    if (isTimeSlotBooked(date, timeSlot, dermatologist)) {
                        System.out.println("This time slot is already booked. Please select another slot.");
                    } else {
                        break;
                    }
                } else {
                    System.out.println("Invalid slot selection. Try again.");
                }
            }

            scanner.nextLine();

            // Confirm Registration Fee
            System.out.println("\nA registration fee of LKR " + REGISTRATION_FEE + " is required.");
            System.out.print("Confirm by typing 'yes' to proceed: ");
            String confirmation = scanner.nextLine();
            if (!confirmation.equalsIgnoreCase("yes")) {
                System.out.println("Appointment not made. Registration fee was not confirmed.");
                return;
            }

            // select multiple treatments
            System.out.println("\n--- Select Treatments ---");
            System.out.println("Available Treatments:");
            treatmentPrices.forEach((treatment, price) -> System.out.println("- " + treatment + ": LKR " + price));

            List<String> selectedTreatments = new ArrayList<>();
            while (true) {
                System.out.print("Enter a treatment name to add (or type 'done' to finish): ");
                String treatment = scanner.nextLine();

                if (treatment.equalsIgnoreCase("done")) break;
                if (treatmentPrices.containsKey(treatment)) {
                    selectedTreatments.add(treatment);
                } else {
                    System.out.println("Invalid treatment name. Please try again.");
                }
            }

            // Calculate the total cost with rounding up to the nearest decimal
            double treatmentTotal = selectedTreatments.stream().mapToDouble(treatmentPrices::get).sum();
            double unroundedTotalCost = REGISTRATION_FEE + treatmentTotal + (treatmentTotal * TAX_RATE);
            double totalCost = Math.ceil(unroundedTotalCost * 100) / 100;

           // Display selected treatments and total cost
            System.out.println("\n--- Selected Treatments ---");
            for (String treatment : selectedTreatments) {
                System.out.printf("- %s: LKR %.2f%n", treatment, treatmentPrices.get(treatment));
            }
            System.out.printf("Registration Fee: LKR %.2f%n", REGISTRATION_FEE);
            System.out.printf("Tax (2.5%%): LKR %.2f%n", treatmentTotal * TAX_RATE);
            System.out.printf("Total Cost: LKR %.2f%n", totalCost);

            // Gather Patient Details with Validation
            System.out.print("Enter Patient NIC: ");
            String nic = scanner.nextLine();
            if (!isValidNIC(nic)) {
                System.out.println("Invalid NIC. Appointment not made.");
                return;
            }

            System.out.print("Enter Patient Name: ");
            String name = scanner.nextLine();
            if (!isValidName(name)) {
                System.out.println("Invalid Name. Appointment not made.");
                return;
            }

            System.out.print("Enter Patient Email: ");
            String email = scanner.nextLine();
            if (!isValidEmail(email)) {
                System.out.println("Invalid Email. Appointment not made.");
                return;
            }

            System.out.print("Enter Patient Phone: ");
            String phone = scanner.nextLine();
            if (!isValidPhone(phone)) {
                System.out.println("Invalid Phone Number. Appointment not made.");
                return;
            }

            // Create Patient Object with details
            K2462921_Patient patient = new K2462921_Patient(nic, name, email, phone);

            // Create the Appointment object
            K2462921_Appointment appointment = new K2462921_Appointment(
                    appointmentId,
                    patient,
                    date,
                    selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    dermatologist,
                    selectedTreatments,
                    totalCost
            );

            // Save the appointment to the database
            appointmentDAO.saveAppointment(appointment);

            // Update in-memory appointments
            appointmentsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(appointment);

            System.out.println("\nAppointment made successfully!");
            generateInvoice(appointment);

        } catch (InputMismatchException e) {
            System.out.println("Invalid input format.");
            scanner.next(); // Consume invalid input
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    // Validation methods
    private boolean isValidNIC(String nic) {
        return nic != null && nic.matches("\\d{9}[Vv]"); // Example pattern for Sri Lankan NIC
    }

    private boolean isValidName(String name) {
        return name != null && name.matches("[a-zA-Z\\s]+");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // Utility method to get available time slots based on the day of the week
    private List<LocalTime> getAvailableSlots(String dayOfWeek) {
        switch (dayOfWeek.toUpperCase()) {
            case "MONDAY":
                return generateSlots(LocalTime.of(10, 0), LocalTime.of(13, 0));
            case "WEDNESDAY":
                return generateSlots(LocalTime.of(14, 0), LocalTime.of(17, 0));
            case "FRIDAY":
                return generateSlots(LocalTime.of(16, 0), LocalTime.of(20, 0));
            case "SATURDAY":
                return generateSlots(LocalTime.of(9, 0), LocalTime.of(13, 0));
            default:
                return new ArrayList<>();
        }
    }

    // Generate 15-minute interval slots
    private List<LocalTime> generateSlots(LocalTime start, LocalTime end) {
        List<LocalTime> slots = new ArrayList<>();
        while (start.isBefore(end)) {
            slots.add(start);
            start = start.plusMinutes(15);
        }
        return slots;
    }

    // Check if a specific time slot on a date is booked
    private boolean isTimeSlotBooked(String date, String timeSlot, String dermatologist) {
        ArrayList<K2462921_Appointment> dailyAppointments = appointmentsByDate.get(date);
        if (dailyAppointments != null) {
            for (K2462921_Appointment appointment : dailyAppointments) {
                if (appointment.getTime().equals(timeSlot) && appointment.getDermatologist().equals(dermatologist)) {
                    return true; // Slot is booked
                }
            }
        }
        return false; // Slot is available
    }

    // New method to view appointments by a specified date
    public void viewAppointmentsByDate() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter the date (yyyy-MM-dd) to view appointments: ");
        String date = scanner.nextLine();

        // Validate date format
        if (!isValidDateFormat(date)) {
            System.out.println("Invalid date format. Please enter the date in yyyy-MM-dd format.");
            return;
        }

        List<K2462921_Appointment> appointmentsOnDate = appointmentsByDate.getOrDefault(date, new ArrayList<>());

        // Check if there are appointments on that date
        if (appointmentsOnDate.isEmpty()) {
            System.out.println("No appointments found on " + date);
        } else {
            System.out.println("\nAppointments on " + date + ":");
            for (K2462921_Appointment appointment : appointmentsOnDate) {
                System.out.println("Appointment ID: " + appointment.getAppointmentId());
                System.out.println("Patient NIC: " + appointment.getPatient().getNic());
                System.out.println("Patient Name: " + appointment.getPatient().getName());
                System.out.println("Doctor: " + appointment.getDermatologist());
                System.out.println("Time Slot: " + appointment.getTime());
                System.out.println("\n--- Selected Treatments ---");
                double treatmentTotal = 0.0;
                for (String treatment : appointment.getTreatments()) {
                    double price = treatmentPrices.getOrDefault(treatment, 0.0);
                    treatmentTotal += price;
                    System.out.printf("- %s: LKR %.2f%n", treatment, price);
                }

                System.out.printf("Registration Fee: LKR %.2f%n", REGISTRATION_FEE);
                double taxAmount = treatmentTotal * TAX_RATE;
                System.out.printf("Tax (2.5%%): LKR %.2f%n", taxAmount);

                double totalCost = REGISTRATION_FEE + treatmentTotal + taxAmount;
                System.out.printf("Total Amount: LKR %.2f%n", totalCost);

                System.out.println(".............................................................................");
                System.out.println("  ");
            }
        }
    }

    // Helper method to validate date format
    private boolean isValidDateFormat(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(date);  // Will throw ParseException if invalid
            return true;
        } catch (ParseException e) {
            return false;
        }
    }


    // Method to search for an appointment by ID or Patient's Name
    private void searchAppointments() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter appointment ID or Patient's Name to search: ");
        String searchParam = scanner.nextLine();

        // Call the DAO method to search appointments in the database
        List<K2462921_Appointment> foundAppointments = appointmentDAO.searchAppointmentsInDatabase(searchParam);

        if (foundAppointments.isEmpty()) {
            System.out.println("No appointments found for the given search parameter.");
        } else {
            System.out.println("\nAppointment Found:");
            for (K2462921_Appointment appointment : foundAppointments) {
                System.out.println("Appointment ID: " + appointment.getAppointmentId());
                System.out.println("NIC: " + appointment.getPatient().getNic());
                System.out.println("Patient Name: " + appointment.getPatient().getName());
                System.out.println("Doctor: " + appointment.getDermatologist());
                System.out.println("Date: " + appointment.getDate());
                System.out.println("Time Slot: " + appointment.getTime());
                System.out.println("Email: " + appointment.getPatient().getEmail());
                System.out.println("Phone: " + appointment.getPatient().getPhone());
                System.out.println("\n--- Selected Treatments ---");
                double treatmentTotal = 0.0;
                for (String treatment : appointment.getTreatments()) {
                    double price = treatmentPrices.getOrDefault(treatment, 0.0);
                    treatmentTotal += price;
                    System.out.printf("- %s: LKR %.2f%n", treatment, price);
                }

                System.out.printf("Registration Fee: LKR %.2f%n", REGISTRATION_FEE);
                double taxAmount = treatmentTotal * TAX_RATE;
                System.out.printf("Tax (2.5%%): LKR %.2f%n", taxAmount);

                double totalCost = REGISTRATION_FEE + treatmentTotal + taxAmount;
                System.out.printf("Total Amount: LKR %.2f%n", totalCost);
                System.out.println();
            }
        }
    }
    // Generate an invoice after successful appointment creation
    private void generateInvoice(K2462921_Appointment appointment) {
        System.out.println("\n--- Invoice ---");
        System.out.printf("Appointment ID: %s%n", appointment.getAppointmentId());
        System.out.printf("Patient NIC: %s%n", appointment.getPatient().getNic());
        System.out.printf("Patient Name: %s%n", appointment.getPatient().getName());
        System.out.printf("Patient Phone: %s%n", appointment.getPatient().getPhone());
        System.out.printf("Doctor: %s%n", appointment.getDermatologist());
        System.out.printf("Date: %s%n", appointment.getDate());
        System.out.printf("Time Slot: %s%n", appointment.getTime());

        System.out.println("\n--- Selected Treatments ---");
        double treatmentTotal = 0.0;
        for (String treatment : appointment.getTreatments()) {
            double price = treatmentPrices.getOrDefault(treatment, 0.0);
            treatmentTotal += price;
            System.out.printf("- %s: LKR %.2f%n", treatment, price);
        }

        System.out.printf("Registration Fee: LKR %.2f%n", REGISTRATION_FEE);
        double taxAmount = treatmentTotal * TAX_RATE;
        System.out.printf("Tax (2.5%%): LKR %.2f%n", taxAmount);

        double totalCost = REGISTRATION_FEE + treatmentTotal + taxAmount;
        totalCost = Math.ceil(totalCost * 100) / 100.0;  // Rounds up to the nearest decimal
        System.out.printf("Total Amount: LKR %.2f%n", totalCost);

        System.out.println("Thank you for choosing Aurora Skin Care. We look forward to your visit!");

        System.out.println(".............................................................................");
        System.out.println("  ");
    }


    private static final Scanner scanner = new Scanner(System.in);

     // Main update flow triggered by option 3 selection
    public void startUpdateProcess() {
        System.out.println("You have selected option 3: Update an Appointment.");

        // Prompt for appointment ID
        System.out.print("Please enter the Appointment ID to update: ");
        String appointmentId = scanner.nextLine();

        // Proceed with update if appointment exists
        if (updateAppointment(appointmentId)) {
            System.out.println("Appointment has been updated successfully.");
        } else {
            System.out.println("Failed to update the appointment. Please check the ID or try again.");

        }
    }
    public boolean updateAppointment(String appointmentId) {
        // Fetch the appointment from the DAO by appointmentId
        Optional<K2462921_Appointment> optionalAppointment = appointmentDAO.getAppointmentById(appointmentId);

        if (!optionalAppointment.isPresent()) {
            System.out.println("Appointment not found with ID: " + appointmentId);
            return false; // Exit if appointment not found
        }

        K2462921_Appointment appointment = optionalAppointment.get();

        // Display current appointment details
        displayCurrentDetails(appointment);

        // Loop to keep allowing updates until "done" is selected
        boolean done = false;
        while (!done) {
            System.out.println("\nSelect the field to update:");
            System.out.println("1. Dermatologist");
            System.out.println("2. Date");
            System.out.println("3. Time Slot");
            System.out.println("4. Treatments");
            System.out.println("5. Done");
            System.out.print("Enter choice to update: ");
            int choice = scanner.nextInt();
            scanner.nextLine();  // Consume the newline character after integer input

            switch (choice) {
                case 1:
                    updateDermatologist(appointment);
                    break;

                case 2:
                    updateDate(appointment);
                    break;

                case 3:
                    updateTimeSlot(appointment);
                    break;

                case 4:
                    updateTreatments(appointment);
                    break;

                case 5:
                    done = true;  // Exit the loop when "done" is selected
                    break;

                default:
                    System.out.println("Invalid choice. No updates made.");
                    break;
            }

            if (!done) {
                // Recalculate total cost and tax after each update
                recalculateTotalCost(appointment);

                // Ask if the user wants to continue
                if (!promptUser("Do you want to continue updating?")) {
                    done = true;
                }
                generateUpdatedInvoice(appointmentId);
            }
        }

        // Final confirmation and update
        if (promptUser("Do you want to update your appointment with these new details")) {
            boolean isUpdated = appointmentDAO.updateAppointmentInDatabase(appointment);
            if (isUpdated) {
                System.out.println("Appointment updated successfully!");
                generateUpdatedInvoice(appointmentId); // Generate and display updated invoice
                return true;
            } else {
                System.out.println("Failed to update appointment.");
                return false;
            }
        } else {
            System.out.println("Update canceled.");
            return false;
        }
    }

    private void updateDermatologist(K2462921_Appointment appointment) {
        System.out.println("\n--- Select Dermatologist ---");
        System.out.println("1. Dr. Ariyathunga");
        System.out.println("2. Dr. Jayaweera");
        System.out.print("Enter choice: ");

        String newDermatologist = switch (scanner.nextInt()) {
            case 1 -> "Dr. Ariyathunga";
            case 2 -> "Dr. Jayaweera";
            default -> throw new IllegalArgumentException("Invalid choice for dermatologist.");
        };
       // appointment.setDermatologist(newDermatologist);
    }

    private void updateDate(K2462921_Appointment appointment) {
        System.out.print("\nEnter Date (yyyy-mm-dd): ");
        String newDate = scanner.nextLine();

        // Display available time slots based on the day
        String dayOfWeek = LocalDate.parse(newDate).getDayOfWeek().toString();
        List<LocalTime> availableSlots = getAvailableSlots(dayOfWeek);

        if (availableSlots.isEmpty()) {
            System.out.println("No available slots on " + dayOfWeek + ". Choose another day.");
        } else {
            // Prompt the user to select a time slot
            System.out.println("Available time slots:");
            for (int i = 0; i < availableSlots.size(); i++) {
                System.out.println((i + 1) + ". " + availableSlots.get(i).toString());
            }
            System.out.print("Choose a time slot: ");
            int slotChoice = scanner.nextInt();
            scanner.nextLine();  // Consume the newline character

            LocalTime selectedSlot = availableSlots.get(slotChoice - 1);
            if (isTimeSlotBooked(newDate, String.valueOf(selectedSlot), appointment.getDermatologist())) {
                appointment.setDate(newDate);
                appointment.setTimeSlot(selectedSlot.toString());
            } else {
                System.out.println("This slot is already booked. Please select another slot.");
            }
        }
    }

    private void updateTimeSlot(K2462921_Appointment appointment) {
        System.out.print("Enter new Time Slot (HH:mm): ");
        String newTimeSlot = scanner.nextLine();
        appointment.setTimeSlot(newTimeSlot);
    }

    private void updateTreatments(K2462921_Appointment appointment) {
        // Display the available treatments with a number
        System.out.println("\n--- Available Treatments ---");
        System.out.println("1. Acne Treatment (LKR 2750.00)");
        System.out.println("2. Skin Whitening (LKR 7650.00)");
        System.out.println("3. Mole Removal (LKR 3850.00)");
        System.out.println("4. Laser Treatment (LKR 12,500.00)");
        System.out.println("Enter the number corresponding to the treatment(s) you want to select (comma separated for multiple): ");

        // Get the user's selection (e.g., "1,3" for Acne and Mole Removal)
        String treatmentsInput = scanner.nextLine();
        String[] selectedTreatmentIndexes = treatmentsInput.split(",");

        // Create a list to store the selected treatments
        List<String> selectedTreatments = new ArrayList<>();

        // Add treatments based on the user's selections
        for (String index : selectedTreatmentIndexes) {
            switch (index.trim()) {
                case "1":
                    selectedTreatments.add("Acne Treatment");
                    break;
                case "2":
                    selectedTreatments.add("Skin Whitening");
                    break;
                case "3":
                    selectedTreatments.add("Mole Removal");
                    break;
                case "4":
                    selectedTreatments.add("Laser Treatment");
                    break;
                default:
                    System.out.println("Invalid choice: " + index.trim());
                    break;
            }
        }

        // Set the selected treatments in the appointment
        appointment.setTreatments(selectedTreatments);
        System.out.println("Selected treatments: " + selectedTreatments);
    }

    private boolean promptUser(String message) {
        System.out.print(message + " (yes/no): ");
        String userResponse = scanner.nextLine();  // Get user input
        return userResponse.equals("yes");  // Check if input is exactly "yes" (case-sensitive)
    }

    private Object displayCurrentDetails(K2462921_Appointment appointment) {
        System.out.println("Current details for Appointment ID: " + appointment.getAppointmentId());
        System.out.println("Doctor: " + appointment.getDermatologist());
        System.out.println("Date: " + appointment.getDate());
        System.out.println("Time: " + appointment.getTime());
        System.out.println("Patient NIC: " + appointment.getPatient().getNic());
        System.out.println("Patient Name: " + appointment.getPatient().getName());
        System.out.println("Patient Email: " + appointment.getPatient().getEmail());

        System.out.println("\n--- Selected Treatments ---");
        double treatmentTotal = 0.0;
        for (String treatment : appointment.getTreatments()) {
            double price = treatmentPrices.getOrDefault(treatment, 0.0);
            treatmentTotal += price;
            System.out.printf("- %s: LKR %.2f%n", treatment, price);
        }

        System.out.printf("Registration Fee: LKR %.2f%n", REGISTRATION_FEE);
        double taxAmount = treatmentTotal * TAX_RATE;
        System.out.printf("Tax (2.5%%): LKR %.2f%n", taxAmount);

        double totalCost = REGISTRATION_FEE + treatmentTotal + taxAmount;
        totalCost = Math.ceil(totalCost * 100) / 100.0;  // Rounds up to the nearest decimal
        System.out.printf("Total Amount: LKR %.2f%n", totalCost);
        return null;
    }

    private void recalculateTotalCost(K2462921_Appointment appointment) {
        List<String> treatments = appointment.getTreatments();

        // If treatments is null, initialize it as an empty list to avoid NullPointerException
        if (treatments == null) {
            treatments = new ArrayList<>();
        }

        // Calculate the total treatment cost by summing up the prices from treatmentPrices map
        double treatmentTotal = treatments.stream()
                .mapToDouble(treatment -> treatmentPrices.getOrDefault(treatment, 0.0)) // Use getOrDefault to handle missing treatments in the map
                .sum();

        double totalCost = REGISTRATION_FEE + treatmentTotal + (treatmentTotal * TAX_RATE);
        appointment.setTotalCost(totalCost);
    }

    public void generateUpdatedInvoice(String appointmentId) {
        Optional<K2462921_Appointment> updatedAppointment = appointmentDAO.getUpdatedAppointmentFromDatabase(appointmentId);

        if (updatedAppointment.isPresent()) {
            K2462921_Appointment appointment = updatedAppointment.get();

            // Ensure patient is not null before proceeding
            if (appointment.getPatient() != null) {
                System.out.println("Generating Updated Invoice:");
                displayCurrentDetails(appointment); // Display updated details

                // Display updated total cost with treatments
                System.out.println("Updated Total Cost: " + appointment.getTotalCost());
            }
        } else {
            System.out.println("Failed to retrieve updated appointment for invoice generation.");
        }
    }



}



