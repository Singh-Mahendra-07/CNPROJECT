#!/usr/bin/env python3
"""
TCP Server for Complex Number Operations
Supports: Exponential, Logarithm, and Power operations with encryption
"""

import socket
import json
import cmath
from cryptography.fernet import Fernet

# Server configuration
HOST = '127.0.0.1'
PORT = 65432
KEY_FILE = 'secret.key'


def generate_key():
    """Generate and save encryption key"""
    key = Fernet.generate_key()
    with open(KEY_FILE, 'wb') as key_file:
        key_file.write(key)
    return key


def load_key():
    """Load encryption key from file"""
    try:
        with open(KEY_FILE, 'rb') as key_file:
            return key_file.read()
    except FileNotFoundError:
        return generate_key()


def exponential(complex_num):
    """Calculate exponential of a complex number (e^z)"""
    result = cmath.exp(complex_num)
    return result


def logarithm(complex_num):
    """Calculate natural logarithm of a complex number"""
    if complex_num == 0:
        raise ValueError("Logarithm of zero is undefined")
    result = cmath.log(complex_num)
    return result


def power(complex_num, exponent):
    """Calculate power of a complex number (z^n)"""
    result = complex_num ** exponent
    return result


def format_complex(real, imag):
    """Format complex number as a string with proper sign handling"""
    if imag >= 0:
        return f'{real} + {imag}i'
    else:
        return f'{real} - {abs(imag)}i'


def format_complex_result(result):
    """Format complex result with proper sign handling"""
    if result.imag >= 0:
        return f'{result.real:.6f} + {result.imag:.6f}i'
    else:
        return f'{result.real:.6f} - {abs(result.imag):.6f}i'


def process_request(data):
    """Process client request and perform the operation"""
    try:
        request = json.loads(data)
        operation = request.get('operation')
        real = request.get('real')
        imag = request.get('imag')
        
        # Create complex number
        complex_num = complex(real, imag)
        
        if operation == 'exponential':
            result = exponential(complex_num)
            return {
                'success': True,
                'operation': 'exponential',
                'input': format_complex(real, imag),
                'result_real': result.real,
                'result_imag': result.imag,
                'result_str': format_complex_result(result)
            }
        
        elif operation == 'logarithm':
            result = logarithm(complex_num)
            return {
                'success': True,
                'operation': 'logarithm',
                'input': format_complex(real, imag),
                'result_real': result.real,
                'result_imag': result.imag,
                'result_str': format_complex_result(result)
            }
        
        elif operation == 'power':
            exponent = request.get('exponent')
            if exponent is None:
                return {'success': False, 'error': 'Exponent required for power operation'}
            
            # Exponent can be complex or real
            if isinstance(exponent, dict):
                exp_value = complex(exponent.get('real', 0), exponent.get('imag', 0))
            else:
                exp_value = exponent
            
            result = power(complex_num, exp_value)
            return {
                'success': True,
                'operation': 'power',
                'input': format_complex(real, imag),
                'exponent': str(exp_value),
                'result_real': result.real,
                'result_imag': result.imag,
                'result_str': format_complex_result(result)
            }
        
        else:
            return {'success': False, 'error': f'Unknown operation: {operation}'}
    
    except Exception as e:
        return {'success': False, 'error': str(e)}


def main():
    """Main server function"""
    # Load or generate encryption key
    key = load_key()
    cipher = Fernet(key)
    
    print(f"[SERVER] Starting server on {HOST}:{PORT}")
    print(f"[SERVER] Encryption key loaded from {KEY_FILE}")
    
    # Create TCP socket
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server_socket.bind((HOST, PORT))
        server_socket.listen()
        
        print("[SERVER] Server is listening for connections...")
        
        while True:
            try:
                conn, addr = server_socket.accept()
                with conn:
                    print(f"\n[SERVER] Connected by {addr}")
                    
                    # Receive encrypted data
                    encrypted_data = conn.recv(4096)
                    if not encrypted_data:
                        continue
                    
                    # Decrypt data
                    try:
                        decrypted_data = cipher.decrypt(encrypted_data)
                        data_str = decrypted_data.decode('utf-8')
                        print(f"[SERVER] Received (decrypted): {data_str}")
                    except Exception as e:
                        print(f"[SERVER] Decryption error: {e}")
                        continue
                    
                    # Process request
                    response = process_request(data_str)
                    response_str = json.dumps(response)
                    print(f"[SERVER] Response: {response_str}")
                    
                    # Encrypt response
                    encrypted_response = cipher.encrypt(response_str.encode('utf-8'))
                    
                    # Send encrypted response
                    conn.sendall(encrypted_response)
                    print(f"[SERVER] Sent encrypted response to {addr}")
            
            except KeyboardInterrupt:
                print("\n[SERVER] Shutting down server...")
                break
            except Exception as e:
                print(f"[SERVER] Error: {e}")


if __name__ == '__main__':
    main()
