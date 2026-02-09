CREATE TABLE IF NOT EXISTS toll_email_meta (
    id UUID PRIMARY KEY,
    gmail_id TEXT NOT NULL,
    sender_email TEXT NOT NULL,
    toll_paid BOOLEAN NOT NULL DEFAULT FALSE,
    stripe_customer_id TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_toll_email_meta_gmail_id ON toll_email_meta(gmail_id);
CREATE INDEX IF NOT EXISTS idx_toll_email_meta_sender_email ON toll_email_meta(sender_email);
CREATE INDEX IF NOT EXISTS idx_toll_email_meta_created_at ON toll_email_meta(created_at);
