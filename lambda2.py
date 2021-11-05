import json
import boto3
import hashlib
import re

# getLogMessages Lambda

def lambda_handler(event, context):
    
    # Initialize AWS S3 SDK for Python which is Boto3
    s3 = boto3.client('s3')
    
    # Pre-compile the regex for quicker matching later in loop
    pattern = re.compile("([a-c][e-g][0-3]|[A-Z][5-9][f-w]){5,15}")
    
    # Input can come either from query string parameter for GET request or request body from POST request
    if event['queryStringParameters']:
        time = event['queryStringParameters']['T']
        timeInterval = event['queryStringParameters']['dT']
    else:
        time = json.loads(event['body'])['T']
        timeInterval = json.loads(event['body'])['dT']
    
    # Input bucket and file from own S3
    response = s3.get_object(Bucket ="logfilehomework2sai", Key="input/input.log")

    newList = list()
    
    # Perform data loading
    newList = response.get('Body').read().decode('utf-8')
    newList = "".join(newList).replace("\r", "").split("\n")
    
    # Last line is always empty
    newList.pop()
    
    # Form time constants for given input, base everything to seconds for direct comparison
    base = int(time[0:2]) * 60 * 60 + int(time[3:5]) * 60 + int(time[6:8])
    differential = int(timeInterval[0:2]) * 60 * 60 + int(timeInterval[3:5]) * 60 + int(timeInterval[6:8])
    lowTimeInterval = base - differential
    highTimeInterval = base + differential
    
    # Binary search to find message at given time
    low = 0
    mid = 0
    high = len(newList) - 1
    
    while low <= high:
        mid = (low+high)//2
        
        current = int(newList[mid][0:2]) * 60 * 60 + int(newList[mid][3:5]) * 60 + int(newList[mid][6:8])
        if (current > base):
            high = mid - 1
        elif (current < base):
            low = mid + 1
        else:
            break
    
    listIndexAtTime = mid
    
    # Binary search in lower half of the list to find lower time interval of the whole input timeframe
    low = 0
    high = mid
    
    while low <= high:
        mid = (low+high)//2
        
        current = int(newList[mid][0:2]) * 60 * 60 + int(newList[mid][3:5]) * 60 + int(newList[mid][6:8])
        if (current > lowTimeInterval):
            high = mid - 1
        elif (current < lowTimeInterval):
            low = mid + 1
        else:
            break
    
    listIndexAtLow = mid
    
    # Binary search in upper half of the list to find higher time interval of the whole input timeframe
    low = mid
    high = len(newList) - 1
    
    while low <= high:
        mid = (low+high)//2
        
        current = int(newList[mid][0:2]) * 60 * 60 + int(newList[mid][3:5]) * 60 + int(newList[mid][6:8])
        if (current > highTimeInterval):
            high = mid - 1
        elif (current < highTimeInterval):
            low = mid + 1
        else:
            break
    
    finalList = list()

    # Go through log messages only for the given timeframe and match with pre-compiled regex
    for item in range(listIndexAtLow, mid + 1):
        
        # For a match, calculate its MD5 hash and append it to final list to be sent
        if pattern.search(newList[item]):
            finalList.append(hashlib.md5(newList[item].encode('utf-8')).hexdigest())

    # Return the MD5 hashes list as a string for easier ingestion
    return {
        'statusCode': 200,
        'body': ", ".join(finalList)
    }