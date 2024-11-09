package dao;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.K2462921_Appointment;
import models.K2462921_Patient;
import utils.DatabaseConnection;

public class AppointmentDAO {
    private static final Logger logger = Logger.getLogger(AppointmentDAO.class.getName());

    // Method to save a new appointment
    public void saveAppointment(K2462921_Appointment appointment) {
        if (isAppointmentExists(appointment)) {
            logger.log(Level.WARNING, "Appointment already exists for patient: {0} on {1} at {2}",
                    new Object[]{appointment.getPatient().getName(), appointment.getDate(), appointment.getTime()});
            return;
        }

        String sql = "INSERT INTO appointments (date, time, dermatologist, patient_nic, patient_name, patient_email, patient_phone, treatments, total_cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, appointment.getDate());
            preparedStatement.setString(2, appointment.getTime());
            preparedStatement.setString(3, appointment.getDermatologist());
            preparedStatement.setString(4, appointment.getPatient().getNic());
            preparedStatement.setString(5, appointment.getPatient().getName());
            preparedStatement.setString(6, appointment.getPatient().getEmail());
            preparedStatement.setString(7, appointment.getPatient().getPhone());
            preparedStatement.setString(8, String.join(", ", appointment.getTreatments()));
            preparedStatement.setDouble(9, appointment.getTreatmentCost());

            preparedStatement.executeUpdate();
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    logger.log(Level.INFO, "Appointment saved with ID: {0}", id);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception occurred while saving appointment: ", e);
        }
    }

    // Method to check if an appointment already exists
    private boolean isAppointmentExists(K2462921_Appointment appointment) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE date = ? AND time = ? AND patient_nic = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, appointment.getDate());
            preparedStatement.setString(2, appointment.getTime());
            preparedStatement.setString(3, appointment.getPatient().getNic());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception occurred while checking for existing appointment: ", e);
        }
        return false;
    }

    // Method to get the last appointment by ID
    // Method to get the last appointment ID as an Integer
    public Optional<Integer> getLastAppointmentId() {
        String sql = "SELECT id FROM appointments ORDER BY id DESC LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                int lastId = resultSet.getInt("id"); // Retrieve the ID as an integer
                return Optional.of(lastId); // Return only the ID
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception occurred while retrieving the last appointment ID: ", e);
        }
        return Optional.empty(); // Return empty if no appointments found
    }

    // Method to fetch full appointment details by ID
    public Optional<K2462921_Appointment> getAppointmentById(String appointmentId) {
        String sql = "SELECT * FROM appointments WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, appointmentId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                K2462921_Appointment appointment = new K2462921_Appointment(
                        resultSet.getString("id"),
                        new K2462921_Patient(
                                resultSet.getString("patient_nic"),
                                resultSet.getString("patient_name"),
                                resultSet.getString("patient_email"),
                                resultSet.getString("patient_phone")
                        ),
                        resultSet.getString("date"),
                        resultSet.getString("time"),
                        resultSet.getString("dermatologist"),
                        List.of(resultSet.getString("treatments").split(", ")),
                        resultSet.getDouble("total_cost")
                );
                return Optional.of(appointment);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception occurred while retrieving appointment by ID: ", e);
        }
        return Optional.empty(); // Return empty if no appointment found with the given ID
    }

    // Method to search appointments by ID, date, or patient name
    public List<K2462921_Appointment> searchAppointmentsInDatabase(String searchParam) {
        List<K2462921_Appointment> appointments = new ArrayList<>();
        String sql;
        boolean isNumeric = searchParam.matches("\\d+");

        boolean isDate;
        try {
            Date.valueOf(searchParam);
            isDate = true;
        } catch (IllegalArgumentException e) {
            isDate = false;
        }

        if (isNumeric) {
            sql = "SELECT * FROM appointments WHERE id = ?";
        } else if (isDate) {
            sql = "SELECT * FROM appointments WHERE date = ?";
        } else {
            sql = "SELECT * FROM appointments WHERE patient_name LIKE ?";
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            if (isNumeric) {
                preparedStatement.setInt(1, Integer.parseInt(searchParam));
            } else if (isDate) {
                preparedStatement.setDate(1, Date.valueOf(searchParam));
            } else {
                preparedStatement.setString(1, "%" + searchParam + "%");
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                K2462921_Appointment appointment = new K2462921_Appointment(
                        resultSet.getString("id"),
                        new K2462921_Patient(
                                resultSet.getString("patient_nic"),
                                resultSet.getString("patient_name"),
                                resultSet.getString("patient_email"),
                                resultSet.getString("patient_phone")
                        ),
                        resultSet.getString("date"),
                        resultSet.getString("time"),
                        resultSet.getString("dermatologist"),
                        List.of(resultSet.getString("treatments").split(", ")),
                        resultSet.getDouble("total_cost")
                );
                appointments.add(appointment);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception occurred while searching for appointments: ", e);
        }
        return appointments;
    }

    // Method to update an existing appointment in the database
    public boolean updateAppointmentInDatabase(K2462921_Appointment appointment) {
        String sql = "UPDATE appointments SET date = ?, time = ?, dermatologist = ?, patient_nic = ?, " +
                "patient_name = ?, patient_email = ?, patient_phone = ?, treatments = ?, total_cost = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, appointment.getDate());
            preparedStatement.setString(2, appointment.getTime());
            preparedStatement.setString(3, appointment.getDermatologist());
            preparedStatement.setString(4, appointment.getPatient().getNic());
            preparedStatement.setString(5, appointment.getPatient().getName());
            preparedStatement.setString(6, appointment.getPatient().getEmail());
            preparedStatement.setString(7, appointment.getPatient().getPhone());
            preparedStatement.setString(8, String.join(", ", appointment.getTreatments()));

            // Check for null or empty value in getTotalCost, and provide a default value
            double totalCost = (appointment.getTotalCost() != null && !appointment.getTotalCost().trim().isEmpty())
                    ? Double.parseDouble(appointment.getTotalCost().trim())
                    : 0.0; // Default to 0.0 if null or empty
            preparedStatement.setDouble(9, totalCost);

            preparedStatement.setString(10, appointment.getAppointmentId()); // ID for the WHERE clause

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Appointment updated successfully with ID: {0}", appointment.getAppointmentId());
                return true;
            } else {
                logger.log(Level.WARNING, "No appointment found with ID: {0}", appointment.getAppointmentId());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception occurred while updating appointment: ", e);
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Number format exception occurred while parsing total cost for appointment ID: " + appointment.getAppointmentId(), e);
        }
        return false;
    }

    // Method to retrieve the updated appointment from the database
    public static Optional<K2462921_Appointment> getUpdatedAppointmentFromDatabase(String appointmentId) {
        String sql = "SELECT * FROM appointments WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, appointmentId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                K2462921_Appointment updatedAppointment = new K2462921_Appointment();
                updatedAppointment.setId(resultSet.getString("id"));
                updatedAppointment.setDate(resultSet.getString("date"));
                updatedAppointment.setTime(resultSet.getString("time"));
                updatedAppointment.setDermatologist(resultSet.getString("dermatologist"));
                updatedAppointment.setTotalCost(resultSet.getDouble("total_cost"));

                K2462921_Patient patient = new K2462921_Patient();
                patient.setNic(resultSet.getString("patient_nic"));
                patient.setName(resultSet.getString("patient_name"));
                patient.setEmail(resultSet.getString("patient_email"));
                patient.setPhone(resultSet.getString("patient_phone"));
                updatedAppointment.setPatient(patient);

                String treatments = resultSet.getString("treatments");
                if (treatments != null) {
                    updatedAppointment.setTreatments(Arrays.asList(treatments.split(",\\s*")));
                } else {
                    updatedAppointment.setTreatments(Collections.emptyList()); // Handle null treatments
                }

                return Optional.of(updatedAppointment);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception occurred while fetching updated appointment: ", e);
        }

        return Optional.empty();
    }




}

