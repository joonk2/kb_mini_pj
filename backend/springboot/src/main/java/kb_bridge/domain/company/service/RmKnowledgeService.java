package kb_bridge.domain.company.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kb_bridge.domain.company.entity.Company;

@Service
public class RmKnowledgeService {

    private final Path csvPath;

    public RmKnowledgeService(
            @Value("${rm.data.path:../../RM_Prior_Knowledge_100.csv}") String csvPath
    ) {
        this.csvPath = Path.of(csvPath);
    }

    public List<Company> findAll() {
        try {
            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IllegalStateException("RM knowledge CSV is empty: " + csvPath);
            }

            List<Company> companies = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                if (!lines.get(i).isBlank()) {
                    companies.add(toCompany(parseCsvLine(lines.get(i)), i + 1));
                }
            }
            return List.copyOf(companies);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read RM knowledge CSV: " + csvPath, e);
        }
    }

    public Company findById(String companyId) {
        return findAll().stream()
                .filter(company -> company.companyId().equals(companyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RM company id: " + companyId));
    }

    private Company toCompany(List<String> columns, int lineNumber) {
        if (columns.size() != 7) {
            throw new IllegalStateException(
                    "Expected 7 CSV columns at line " + lineNumber + " in " + csvPath);
        }
        try {
            return new Company(
                    columns.get(0),
                    columns.get(1),
                    LocalDate.parse(columns.get(2)),
                    columns.get(3),
                    columns.get(4),
                    columns.get(5),
                    columns.get(6)
            );
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid RM knowledge CSV at line " + lineNumber, e);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                columns.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) {
            throw new IllegalStateException("Unclosed quoted value in RM knowledge CSV");
        }
        columns.add(value.toString());
        return columns;
    }
}
