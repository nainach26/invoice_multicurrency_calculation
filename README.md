This deployment strategy employs a hybrid-cloud approach designed to maximize developer velocity while maintaining enterprise-grade backend security. By leveraging Vercel for the frontend and AWS ECS Fargate for the backend, the architecture ensures a highly scalable, "pay-as-you-go" environment that is fully automated via Terraform and GitHub Actions.

1. Frontend Hosting: Vercel (vs. AWS S3/CloudFront)
* Platform: Vercel
* Why: Since the frontend is a Next.js application, Vercel is the optimal choice. It offers native support for Next.js features, edge-side rendering, and automated global distribution (CDN).
* Efficiency: It simplifies the developer workflow with "push-to-deploy" functionality and automatic SSL management, removing the need for manual S3/CloudFront configuration.

2. Backend Hosting: AWS ECS Fargate (vs. App Runner or Lambda)
* Platform: Amazon ECS with Fargate
* Why: This provides a serverless container environment for the Quarkus (Java) backend. It offers a professional balance between ease of use and granular control over the infrastructure.
* Efficiency: Fargate eliminates the need to manage underlying EC2 servers. Given Quarkus’s low memory footprint, we can use the smallest task sizes (0.25 vCPU) to keep costs extremely low while maintaining high performance.

3. Security: Multi-Layered Protection
* Network Isolation: All backend containers run within Private Subnets inside an AWS VPC. They are not reachable from the public internet.
* API Gateway/ALB: An Application Load Balancer (ALB) serves as the single entry point, routing traffic from the Vercel frontend to the private backend.
* CORS Policy: This Quarkus application is configured with a strict Cross-Origin Resource Sharing (CORS) policy, only accepting requests from the verified Vercel production domain.
* Secret Management: Sensitive information (Frankfurter API keys) can be stored in AWS Secrets Manager and injected into the container at runtime, keeping credentials out of the source code.

4. Scalability & Cost-Effectiveness
* Scaling: The backend utilizes Service Auto Scaling, automatically spinning up more containers if CPU or memory usage spikes. The frontend scales infinitely through Vercel’s global edge network.
* Cost: We use a pay-as-you-go model. During low-traffic periods, the infrastructure cost is minimal, and we avoid the "idle server" costs associated with traditional hosting.

5. Infrastructure as Code (IaC) & Automation
* Tooling: Terraform
* CI/CD Pipeline: A GitHub Actions workflow automates the entire lifecycle:
    * Frontend: Automatically deployed by Vercel upon merging to the main branch.
    * Backend: GitHub Actions builds the Docker image, pushes it to Amazon ECR, and triggers a rolling deployment in ECS Fargate to ensure zero downtime.

