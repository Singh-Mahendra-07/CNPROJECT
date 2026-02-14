#!/usr/bin/env python3
"""
TCP Client for Complex Number Operations
Supports: Exponential, Logarithm, and Power operations with encryption
"""

import socket
import json
from cryptography.fernet import Fernet

# Server configuration
HOST = '127.0.0.1'
PORT = 65432
KEY_FILE = 'secret.key'


def load_key():
    """Load encryption key from file"""
    try:
        with open(KEY_FILE, 'rb') as key_file:
            return key_file.read()
    except FileNotFoundError:
        print(f"Error: {KEY_FILE} not found. Please start the server first to generate the key.")
        return None


def send_request(cipher, request_data):
    """Send encrypted request to server and receive response"""
    try:
        # Create TCP socket
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            client_socket.connect((HOST, PORT))
            
            # Prepare and encrypt request
            request_str = json.dumps(request_data)
            encrypted_request = cipher.encrypt(request_str.encode('utf-8'))
            
            # Send encrypted request
            client_socket.sendall(encrypted_request)
            print(f"[CLIENT] Sent encrypted request")
            
            # Receive encrypted response
            encrypted_response = client_socket.recv(4096)
            
            # Decrypt response
            decrypted_response = cipher.decrypt(encrypted_response)
            response = json.loads(decrypted_response.decode('utf-8'))
            
            return response
    
    except Exception as e:
        return {'success': False, 'error': str(e)}


def display_menu():
    """Display operation menu"""
    print("\n" + "="*50)
    print("Complex Number Operations (TCP Client)")
    print("="*50)
    print("1. Exponential (e^z)")
    print("2. Logarithm (log(z))")
    print("3. Power (z^n)")
    print("4. Exit")
    print("="*50)


def get_complex_input(prompt="Enter complex number"):
    """Get complex number input from user"""
    print(f"\n{prompt}:")
    try:
        real = float(input("  Real part: "))
        imag = float(input("  Imaginary part: "))
        return real, imag
    except ValueError:
        print("Invalid input. Please enter numeric values.")
        return None, None


def exponential_operation(cipher):
    """Handle exponential operation"""
    print("\n--- Exponential of Complex Number (e^z) ---")
    real, imag = get_complex_input("Enter complex number z")
    
    if real is None:
        return
    
    request = {
        'operation': 'exponential',
        'real': real,
        'imag': imag
    }
    
    print(f"\nCalculating e^({real} + {imag}i)...")
    response = send_request(cipher, request)
    display_response(response)


def logarithm_operation(cipher):
    """Handle logarithm operation"""
    print("\n--- Logarithm of Complex Number (log(z)) ---")
    real, imag = get_complex_input("Enter complex number z")
    
    if real is None:
        return
    
    request = {
        'operation': 'logarithm',
        'real': real,
        'imag': imag
    }
    
    print(f"\nCalculating log({real} + {imag}i)...")
    response = send_request(cipher, request)
    display_response(response)


def power_operation(cipher):
    """Handle power operation"""
    print("\n--- Power of Complex Number (z^n) ---")
    real, imag = get_complex_input("Enter complex number z")
    
    if real is None:
        return
    
    print("\nEnter exponent n:")
    print("1. Real exponent")
    print("2. Complex exponent")
    exp_choice = input("Choice (1-2): ")
    
    if exp_choice == '1':
        try:
            exponent = float(input("Enter exponent: "))
        except ValueError:
            print("Invalid input.")
            return
    elif exp_choice == '2':
        exp_real, exp_imag = get_complex_input("Enter complex exponent")
        if exp_real is None:
            return
        exponent = {'real': exp_real, 'imag': exp_imag}
    else:
        print("Invalid choice.")
        return
    
    request = {
        'operation': 'power',
        'real': real,
        'imag': imag,
        'exponent': exponent
    }
    
    print(f"\nCalculating ({real} + {imag}i)^{exponent}...")
    response = send_request(cipher, request)
    display_response(response)


def display_response(response):
    """Display server response"""
    print("\n" + "-"*50)
    if response.get('success'):
        print("✓ Operation successful!")
        print(f"Operation: {response.get('operation', 'N/A')}")
        print(f"Input: {response.get('input', 'N/A')}")
        if 'exponent' in response:
            print(f"Exponent: {response.get('exponent')}")
        print(f"Result: {response.get('result_str', 'N/A')}")
    else:
        print("✗ Operation failed!")
        print(f"Error: {response.get('error', 'Unknown error')}")
    print("-"*50)


def main():
    """Main client function"""
    # Load encryption key
    key = load_key()
    if key is None:
        return
    
    cipher = Fernet(key)
    print(f"[CLIENT] Connected to server at {HOST}:{PORT}")
    print(f"[CLIENT] Encryption enabled")
    
    while True:
        display_menu()
        choice = input("\nEnter your choice (1-4): ")
        
        if choice == '1':
            exponential_operation(cipher)
        elif choice == '2':
            logarithm_operation(cipher)
        elif choice == '3':
            power_operation(cipher)
        elif choice == '4':
            print("\nExiting client. Goodbye!")
            break
        else:
            print("\nInvalid choice. Please try again.")


if __name__ == '__main__':
    main()
