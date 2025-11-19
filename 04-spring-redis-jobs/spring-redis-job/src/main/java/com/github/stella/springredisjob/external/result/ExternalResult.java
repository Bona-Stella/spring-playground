package com.github.stella.springredisjob.external.result;

import jakarta.persistence.*;

@Entity
@Table(name = "external_results")
public class ExternalResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private double temperatureC;

    @Column(nullable = false)
    private String description;

    // 외부 응답이 가진 시각(문자열)
    @Column(nullable = false, length = 64)
    private String fetchedAt;

    // 저장 시각
    @Column(nullable = false, length = 64)
    private String savedAt;

    protected ExternalResult() {}

    public ExternalResult(String city, double temperatureC, String description, String fetchedAt, String savedAt) {
        this.city = city;
        this.temperatureC = temperatureC;
        this.description = description;
        this.fetchedAt = fetchedAt;
        this.savedAt = savedAt;
    }

    public Long getId() { return id; }
    public String getCity() { return city; }
    public double getTemperatureC() { return temperatureC; }
    public String getDescription() { return description; }
    public String getFetchedAt() { return fetchedAt; }
    public String getSavedAt() { return savedAt; }
}
