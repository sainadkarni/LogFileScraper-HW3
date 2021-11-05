import json
import boto3

# logIntervalExists Lambda

def lambda_handler(event, context):
    
    # Input can come either from query string parameter for GET request or request body from POST request
    if event['queryStringParameters']:
        time = event['queryStringParameters']['T']
        timeInterval = event['queryStringParameters']['dT']
    else:
        time = json.loads(event['body'])['T']
        timeInterval = json.loads(event['body'])['dT']
    
    # Initialize AWS S3 SDK for Python which is Boto3
    s3 = boto3.client('s3')
    
    # Input bucket and file from own S3. We only need the starting few bytes and ending few bytes to check if interval slots in.
    # This is done using the Range header from the HTTP specification, the amount of bytes can be lowered even more but this is a safer amount to work with
    responseHead = s3.get_object(Bucket ="logfilehomework2sai", Key="input/input.log", Range="bytes=0-200")
    responseTail = s3.get_object(Bucket ="logfilehomework2sai", Key="input/input.log", Range="bytes=-200")

    headList = list()
    tailList = list()
    
    # Perform data loading
    headList = responseHead.get('Body').read().decode('utf-8')
    headList = "".join(headList).split("\r")[0].replace("\n", "")
    tailList = responseTail.get('Body').read().decode('utf-8')
    tailList = "".join(tailList).split("\r")[-2].replace("\n", "")
    firstMessage = int(headList[0:2]) * 60 + int(headList[3:5])
    lastMessage = int(tailList[0:2]) * 60 + int(tailList[3:5])
    
    # Form time constants for given input, base everything to seconds for direct comparison
    base = int(time[0:2]) * 60 + int(time[3:5])
    differential = int(timeInterval[0:2]) * 60 + int(timeInterval[3:5])
    lowTimeInterval = base - differential
    highTimeInterval = base + differential
    
    # Easier to check for a true case and return everything else as false
    if (firstMessage < lowTimeInterval and lastMessage > highTimeInterval):
        return {
            'statusCode': 200,
            'body': True
        }
    else:
        return {
            'statusCode': 404,
            'body': False
        }