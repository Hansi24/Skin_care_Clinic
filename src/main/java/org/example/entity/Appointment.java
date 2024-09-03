package org.example.entity;

import java.util.List;

public class Appointment {
    private String appointmentId;
    private Patient patient;
    private String date;
    private String time;
    private String dermatologist;
    private List<String> treatments;
    private  double treatmentCost;
    private String totalCost;

    // Constructor for creating an appointment with treatments
    public Appointment(String appointmentId, Patient patient, String date, String time, String dermatologist, List<String> treatments, double treatmentCost) {
        this.appointmentId = appointmentId;
        this.patient = patient;
        this.date = date;
        this.time = time;
        this.dermatologist = dermatologist;
        this.treatments = treatments; // Initialize the treatments
        this.treatmentCost = treatmentCost;
    }

    public Appointment(){

    }

    // Getters for the appointment details
    public String getAppointmentId() {
        return appointmentId;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getDermatologist() {
        return dermatologist;
    }

    public List<String> getTreatments() {
        return treatments; // Getter for treatments
    }

    public double getTreatmentCost() {
        return treatmentCost; // Getter for treatment cost
    }

    // Optional: Add a method to return a summary of the treatments
    public String getTreatmentSummary() {
        return String.join(", ", treatments); // Joins treatment names into a single string
    }

    // Optional: Add a method to display appointment details
//    public String displayAppointmentDetails() {
//        return String.format("Appointment ID: %s\nPatient: %s\nDate: %s\nTime: %s\nDermatologist: %s\nTreatments: %s\nTotal Cost: %.2f",
//                appointmentId, patient.getName(), date, time, dermatologist, getTreatmentSummary(), treatmentCost);
//    }



    public void setDate(String date) {
    }

    public void setTime(String time) {
    }

    public void setDermatologist(String dermatologist) {
    }

    public void setTreatments(List<String> treatments) {
    }

    public void setTotalCost(double totalCost) {
    }

    public void setPatient(Patient patient) {
    }

    public void setId(String id) {
    }

    public String getTotalCost() {
        return totalCost;
    }

    public void setTimeSlot(String newTimeSlot) {
    }

    public void setDoctor(String newDate) {
    }
}
