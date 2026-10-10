package mx.cec.crm.exception;

public class MailUnavailableException extends RuntimeException {
    public MailUnavailableException() {
        super("La recuperación por correo no está disponible en este momento. Inténtalo más tarde.");
    }
}

