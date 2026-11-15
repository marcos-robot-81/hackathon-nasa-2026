package db;

import engine.BodySystem;

public record OsdrStudyRef(
    String accession,
    String tier,
    String sourceFileOrApi,
    BodySystem system,
    String title,
    String biologicalSummary
) {}
