package versioncontrolsystem;

public class FailCaseException extends Exception{
    public FailCaseException(String errorMessage) {
        super(errorMessage);
    }
    @Override
    public void printStackTrace() {
        System.out.println(getMessage());
    }
}
