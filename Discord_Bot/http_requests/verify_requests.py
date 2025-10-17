def verify_code_request(user_id: int, code: str):
    """
    Dummy request for /verify.
    Accepts code "123456" for testing, fails otherwise.
    """
    if code == "123456":
        return True, "Your Minecraft account has been linked successfully!"
    return False, "Invalid verification code."
