CREATE TABLE IF NOT EXISTS todo (
                                    id INT GENERATED ALWAYS AS IDENTITY,
                                    description VARCHAR(255) NOT NULL,
                                    completed BOOLEAN NOT NULL,
                                    username VARCHAR(255) NOT NULL
);